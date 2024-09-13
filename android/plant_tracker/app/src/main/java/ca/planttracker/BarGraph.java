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
 * BarGraph class that is no shame ripped off from google fits heart point and step graphs
 */
public class BarGraph extends GraphBase {

  private int animationCounter = 0;

  // Stolen from https://stackoverflow.com/questions/5896234/how-to-use-android-canvas-to-draw-a-rectangle-with-only-topleft-and-topright-cor
  float[] corners = new float[]{
          10, 10,        // Top left radius in px
          10, 10,        // Top right radius in px
          0, 0,          // Bottom right radius in px
          0, 0           // Bottom left radius in px
  };

  public BarGraph(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BarGraph(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /*************** OVERRIDE METHODS *************************/

  @Override
  protected void calculateHitBoxes() {
     super.calculateHitBoxes();
     animationCounter = 0;
  }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    // Draw Data
    for (int i = 0; i < data.size(); i++) {
      final Path path = new Path();
      RectF rect = hitBoxRect.get(i);
      int rectTop = xAxisBottom - Math.min(calculateRelativeY(data.get(i).value), animationCounter);
      path.addRoundRect(rect.left, rectTop, rect.right, rect.bottom, corners, Path.Direction.CW);
      canvas.drawPath(path, dataPaint);
    }

    // TODO interpolated it for all vars
    if (animationCounter < yAxisSize) {
      animationCounter += 1;
      invalidate();
    }
  }
}
