package ca.planttracker;

import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public class GraphBase extends View {

    /******************** Util classes *************************/
    public static class DataPoint {
        public String key;
        public double value;
        public DataPoint(String key, double value) {
            this.key = key;
            this.value = value;
        }
    }

    /******************** Configuration variables ******************/

    // Onclick information
    protected String infoText = null;
    protected String units = "";
    protected List<RectF> hitBoxRect = new ArrayList<>();

    // Data
    protected String title= "";
    protected List<DataPoint> data = new ArrayList<>();
    protected int dataTarget = 30;
    protected float dataMax = 100;
    protected float dataMin = 0;

    // Canvas info
    protected int canvasWidth;
    protected int canvasHeight;
    protected int xAxisBottom;
    protected int yAxisTop;  // Max size in pixels the graph can be.
    protected int yAxisSize;
    protected int labelSteps;


    // Paint information
    protected Paint textPaint = null;
    protected Paint dataPaint = null;
    private boolean initialized = false;
    private int dataPaintColour = Color.rgb(41, 53, 181);


    public GraphBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttr(attrs);
    }

    public GraphBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttr(attrs);
    }

    /******************* OVERRIDABLE METHODS **********************/
    protected void init() {
        textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);
        initialized = true;

        dataPaint = new Paint();
        dataPaint.setColor(dataPaintColour);
        dataPaint.setStrokeWidth(3);

        canvasWidth = getWidth();
        canvasHeight = getHeight();
        xAxisBottom = canvasHeight * 8/10;
        yAxisTop = 50;
        yAxisSize = xAxisBottom - yAxisTop;

        calculateHitBoxes();
    }


    protected void setAttr(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.GraphBase);
        if (ta.hasValue(R.styleable.GraphBase_graph_title)) {
            title = ta.getString(R.styleable.GraphBase_graph_title);
        }
        dataMax = ta.getFloat(R.styleable.GraphBase_graph_max, 100.0f);
        dataMin = ta.getFloat(R.styleable.GraphBase_graph_min, 0f);
        labelSteps = ta.getInteger(R.styleable.GraphBase_graph_label_step, 1);
        if (ta.hasValue(R.styleable.GraphBase_graph_unit)) {
            units = ta.getString(R.styleable.GraphBase_graph_unit);
        }
        dataPaintColour = ta.getColor(R.styleable.GraphBase_graph_data_color, Color.rgb(41, 53, 181));
    }

    protected void calculateHitBoxes() {
        int rectStart = canvasWidth/(data.size() + 1);
        int rectHalfWidth = rectStart / 4;
        hitBoxRect.clear();
        for (int i = 1; i <= data.size(); i++) {
            hitBoxRect.add(new RectF(rectStart * i - rectHalfWidth, yAxisTop, rectStart * i + rectHalfWidth, xAxisBottom));
        }
    }

    /******************** PUBLIC METHODS *************************/
    public void setData(List<DataPoint> data) {
        this.data = data;
        calculateHitBoxes();
        invalidate();
    }
    public void setDataTarget(int dataTarget) { this.dataTarget = dataTarget; }
    public void setDataMax(int dataMax) { this.dataMax = dataMax; }
    public void setDataMin(int dataMin) { this.dataMin = dataMin; }

    /******************* Children Helper functions***************/
    // Calculates Y value relative to xAxis.
    int calculateRelativeY(double value) { return (int)(yAxisSize * value /(dataMax - dataMin)); }
    // Calculates Y value as a pixel on the screen.
    int calculateAbsY(double value) { return xAxisBottom - calculateRelativeY(value); }


    /***************** Overrides for View ************************/

    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (!initialized) { init(); }

        // Title
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(title, (float) 20, 50, textPaint);
        // Draws dashed line dataTarget
        int dashLength = (canvasWidth - 100) / 30;
        int dataTargetY = calculateAbsY(dataTarget);
        canvas.drawText(String.valueOf(dataTarget), 25, dataTargetY + 10, textPaint);
        for (int i = 75; i < canvasWidth - 50; i += dashLength*1.5) {
            canvas.drawLine(i, dataTargetY, min(i + dashLength, canvasWidth - 50), dataTargetY, textPaint);
        }

        // Draw graph labels
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawLine(50, xAxisBottom, canvasWidth - 50, xAxisBottom, textPaint);
        int textY = xAxisBottom + 50;
        for (int i = 0; i < data.size(); i+= labelSteps) {
            RectF rect = hitBoxRect.get(i);
            canvas.drawLine(rect.centerX(), xAxisBottom, rect.centerX(), xAxisBottom + (textY - xAxisBottom) / 3, textPaint);
            canvas.drawText(data.get(i).key, rect.centerX(), textY, textPaint);
        }

        // TODO make fancy with dotted lines and value at top of dotted line
        if (infoText != null) {
            canvas.drawText(infoText, (float) canvasWidth /2, 100, textPaint);
        }

    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            for (int i = 0; i < hitBoxRect.size(); i++) {
                RectF rect = hitBoxRect.get(i);

                if (rect.contains(event.getX(), event.getY())) {
                    infoText = String.format("%.2f %s", data.get(i).value, units);
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
