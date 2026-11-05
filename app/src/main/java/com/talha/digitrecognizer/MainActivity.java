
package com.talha.digitrecognizer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private TextView txtResult;
    private Interpreter interpreter;

    private int inputWidth;
    private int inputHeight;

    private int inputChannels;
    private int outputClasses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = findViewById(R.id.drawingView);
        txtResult = findViewById(R.id.txtResult);

        Button btnPredict = findViewById(R.id.btnPredict);
        Button btnClear = findViewById(R.id.btnClear);

        // Load the pretrained model.
        try {
            ByteBuffer model = loadModelFile("mnist.tflite");
            interpreter = new Interpreter(model);

            // Read the model's expected input dimensions.
            int[] inputShape = interpreter.getInputTensor(0).shape();

            inputHeight = inputShape[1];
            inputWidth = inputShape[2];
            inputChannels = inputShape[3];

            // Read the number of output classes.
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            outputClasses = outputShape[1];

            if (outputClasses != 10) {
                throw new IllegalStateException(
                        "Expected 10 digit classes, but found " + outputClasses);
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Model loading failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            btnPredict.setEnabled(false);
        }

        // Run the model when Predict is tapped.
        btnPredict.setOnClickListener(v -> predictDigit());

        // Clear the drawing and previous prediction.
        btnClear.setOnClickListener(v -> {
            drawingView.clearCanvas();
            txtResult.setText("Your prediction will appear here");
        });
    }

    private ByteBuffer loadModelFile(String fileName) throws IOException {
        InputStream inputStream = getAssets().open(fileName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();

        byte[] modelBytes = outputStream.toByteArray();

        ByteBuffer modelBuffer = ByteBuffer
                .allocateDirect(modelBytes.length)
                .order(ByteOrder.nativeOrder());

        modelBuffer.put(modelBytes);
        modelBuffer.rewind();

        return modelBuffer;
    }

    private void predictDigit() {
        if (interpreter == null) {
            txtResult.setText("The model could not be loaded.");
            return;
        }

        if (drawingView.getWidth() == 0 || drawingView.getHeight() == 0) {
            txtResult.setText("Drawing canvas is not ready.");
            return;
        }

        try {
            // Capture the drawing, including its black background.
            Bitmap drawingBitmap = Bitmap.createBitmap(
                    drawingView.getWidth(),
                    drawingView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(drawingBitmap);
            drawingView.draw(canvas);

            // Resize the drawing to the model's input dimensions.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                    drawingBitmap,
                    inputWidth,
                    inputHeight,
                    true
            );

            // Convert grayscale pixels to normalized float values.

            // Allocate enough space for every pixel and every input channel.
            ByteBuffer inputBuffer = ByteBuffer
                    .allocateDirect(inputWidth * inputHeight * inputChannels * 4)
                    .order(ByteOrder.nativeOrder());

            // Convert each pixel to grayscale and normalize it.
            for (int y = 0; y < inputHeight; y++) {
                for (int x = 0; x < inputWidth; x++) {
                    int pixel = resizedBitmap.getPixel(x, y);

                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;

                    float grayscale = (red + green + blue) / 3.0f;
                    float normalized = grayscale / 255.0f;

                    // Supply a value for each channel expected by the model.
                    for (int c = 0; c < inputChannels; c++) {
                        inputBuffer.putFloat(normalized);
                    }
                }
            }


            inputBuffer.rewind();

            // Store the model's scores for digits 0 through 9.
            float[][] output = new float[1][outputClasses];

            // Run on-device inference.
            interpreter.run(inputBuffer, output);

            // Find the digit with the highest score.
            int predictedDigit = 0;
            float highestScore = output[0][0];

            for (int i = 1; i < outputClasses; i++) {
                if (output[0][i] > highestScore) {
                    highestScore = output[0][i];
                    predictedDigit = i;
                }
            }

            // Display the prediction and its score.
            txtResult.setText(String.format(
                    Locale.US,
                    "Predicted Digit: %d\nModel Score: %.2f%%",
                    predictedDigit,
                    highestScore * 100.0f
            ));

            drawingBitmap.recycle();
            resizedBitmap.recycle();

        } catch (Exception e) {
            txtResult.setText("Prediction failed: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        if (interpreter != null) {
            interpreter.close();
        }

        super.onDestroy();
    }
}
