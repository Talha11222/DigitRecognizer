package com.talha.digitrecognizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrawingView extends View {

    private final Paint paint = new Paint();
    private final Path path = new Path();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Use a black canvas and white drawing strokes.
        setBackgroundColor(Color.BLACK);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(24f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                path.lineTo(x, y);
                invalidate();
                return true;

            default:
                return true;
        }
    }

    public void clearCanvas() {
        path.reset();
        invalidate();
    }
}
