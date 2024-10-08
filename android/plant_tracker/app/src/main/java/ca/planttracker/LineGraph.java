package ca.planttracker;

import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;

/**
 * Line Graph with points and lines connecting adjacent points together.
 */
public class LineGraph extends GraphBase {
    public LineGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*************** OVERRIDE METHODS *************************/
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw Data
        RectF previousPointRect = null;
        for (int i = 0; i < data.size(); i++) {
            RectF rect = hitBoxRect.get(i);
            canvas.drawCircle(rect.centerX(), calculateAbsY(data.get(i).value), 12, dataPaint);

            if (previousPointRect != null) {
                canvas.drawLine(previousPointRect.centerX(), calculateAbsY(data.get(i-1).value), rect.centerX(), calculateAbsY(data.get(i).value), dataPaint);
            }
            previousPointRect = rect;
        }
    }
}
