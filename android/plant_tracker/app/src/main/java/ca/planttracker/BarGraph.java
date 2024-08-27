package ca.planttracker;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * BarGraph class that is no shame ripped off from google fits heart point and step graphs
 */
public class BarGraph extends View {

  public static class DataPoint {
    public String key;
    public double value;
    public DataPoint(String key, double value) {
      this.key = key;
      this.value = value;
    }
  }

  private Paint paint, paintBlue;
  private int canvasWidth;
  private int canvasHeight;
  private boolean initialized = false;
  private String title= "";
  private int animationCounter = 0;
  private String infoText = null;


  private List<DataPoint> data;
  private int dataTarget = 30;
  private float dataMax = 100;
  private float dataMin = 0;

  private List<RectF> dataRects;
  private int rectBottom;
  private int rectMaxTop;
  private int rectStart;

  // Stolen from https://stackoverflow.com/questions/5896234/how-to-use-android-canvas-to-draw-a-rectangle-with-only-topleft-and-topright-cor
  float[] corners = new float[]{
          10, 10,        // Top left radius in px
          10, 10,        // Top right radius in px
          0, 0,          // Bottom right radius in px
          0, 0           // Bottom left radius in px
  };

  public BarGraph(Context context) {
    super(context);
  }

  public BarGraph(Context context, AttributeSet attrs) {
    super(context, attrs);
    setAttr(attrs);
  }

  public BarGraph(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setAttr(attrs);
  }

  public BarGraph(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    setAttr(attrs);
  }

  private void setAttr(AttributeSet attrs) {
    TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.BarGraph);
    if (ta.hasValue(R.styleable.BarGraph_graph_title)) {
      title = ta.getString(R.styleable.BarGraph_graph_title);
    }
    dataMax = ta.getFloat(R.styleable.BarGraph_graph_max, 100.0f);
    dataMin = ta.getFloat(R.styleable.BarGraph_graph_min, 0f);
  }

  private void init() {
    paint = new Paint();
    paintBlue = new Paint();
    paintBlue.setColor(Color.rgb(41, 53, 181));
    canvasWidth = getWidth();
    canvasHeight = getHeight();
    initialized = true;
    paint.setTextSize(32);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setStrokeWidth(4);

    rectStart = canvasWidth/(data.size() + 1);
    rectBottom = canvasHeight * 8 / 10;
    rectMaxTop = 50;
    calculateRects();
  }

  private void calculateRects() {
    int rectWidth = rectStart / 2;
    dataRects = new ArrayList<>();
    for (int i = 1; i <= data.size(); i++) {
      final Path path = new Path();
      int rectSize = rectBottom - rectMaxTop;
      int rectTop = 50;
      dataRects.add(new RectF(rectStart * i - rectWidth / 2, rectTop, rectStart * i + rectWidth / 2, rectBottom));
    }
  }

  public void setData(List<DataPoint> data) {
    this.data = data;
    calculateRects();
  }
  public void setDataTarget(int dataTarget) {
    this.dataTarget = dataTarget;
  }
  public void setDataMax(int dataMax) { this.dataMax = dataMax; }
  public void setDataMin(int dataMin) { this.dataMin = dataMin; }

  @Override
  protected void onDraw(@NonNull Canvas canvas) {
    super.onDraw(canvas);

    if (!initialized) {
      init();
    }

    int textY = rectBottom + 50;
    int rectSize = rectBottom - rectMaxTop;

    paint.setTextAlign(Paint.Align.LEFT);
    canvas.drawText(title, (float) 20, 50, paint);
    paint.setTextAlign(Paint.Align.CENTER);

    // Draws dashed line dataTarget

    int dashLength = (canvasWidth - 100) / 30;
    int dataTargetY = (int)(rectBottom - (rectSize) * dataTarget / (dataMax - dataMin));
    canvas.drawText(String.valueOf(dataTarget), 25, dataTargetY + 10, paint);
    for (int i = 50; i < canvasWidth - 50; i += dashLength*1.5) {
      canvas.drawLine(i, dataTargetY, min(i + dashLength, canvasWidth - 50), dataTargetY, paint);
    }

    canvas.drawLine(50, rectBottom, canvasWidth - 50, rectBottom, paint);
    for (int i = 1; i <= data.size(); i++) {
      final Path path = new Path();
      RectF rect = dataRects.get(i-1);
      int rectTop = rectBottom - (int)Math.min((rectSize) * data.get(i-1).value /(dataMax - dataMin), animationCounter);
      path.addRoundRect(rect.left, rectTop, rect.right, rect.bottom, corners, Path.Direction.CW);
      canvas.drawPath(path, paintBlue);
      canvas.drawLine(rectStart * i, rectBottom, rectStart * i, rectBottom + (textY - rectBottom) / 3, paint);
      canvas.drawText(data.get(i-1).key, rect.centerX(), textY, paint);
    }

    // TODO interpolated it for all vars
    if (animationCounter < rectBottom) {
      animationCounter += 1;
      invalidate();
    }

    if (infoText != null) {
      canvas.drawText(infoText, canvasWidth/2, 100, paint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN){
      for (int i = 0; i < data.size(); i++) {
        RectF rect = dataRects.get(i);
        Log.d("RectCoordinates", "Left: " + rect.left + " Top: " + rect.top + " Right: " + rect.right + " Bottom: " + rect.bottom);

        if (rect.contains(event.getX(), event.getY())) {
          infoText = data.get(i).value + "HRs";
          break;
        }
      }
      invalidate();
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      infoText = null;
      invalidate();
    }
    super.onTouchEvent(event);
    return true;
  }

}
