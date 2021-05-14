package com.github.mikephil.charting.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.Path;
import android.graphics.LinearGradient;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.highlight.Range;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.github.mikephil.charting.model.GradientColor;

import java.util.List;

public class BarChartRenderer extends BarLineScatterCandleBubbleRenderer {

    protected BarDataProvider mChart;

    /**
     * the rect object that is used for drawing the bars
     */
    protected RectF mBarRect = new RectF();

    protected BarBuffer[] mBarBuffers;

    protected Paint mShadowPaint;
    protected Paint mBarBorderPaint;

    /**
     * path that is used for drawing highlight-lines (drawLines(...) cannot be used because of dashes)
     */
    private Path mHighlightLinePath = new Path();

    private Path mRoundedRectsPath = new Path();
    private float[] radiiZeros = new float[] {0, 0, 0, 0, 0, 0, 0, 0};

    public BarChartRenderer(BarDataProvider chart, ChartAnimator animator,
                            ViewPortHandler viewPortHandler) {
        super(animator, viewPortHandler);
        this.mChart = chart;

        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setStyle(Paint.Style.FILL);
        mHighlightPaint.setColor(Color.rgb(0, 0, 0));
        // set alpha after color
        // mHighlightPaint.setAlpha(120);

        mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadowPaint.setStyle(Paint.Style.FILL);

        mBarBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarBorderPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void initBuffers() {

        BarData barData = mChart.getBarData();
        mBarBuffers = new BarBuffer[barData.getDataSetCount()];

        for (int i = 0; i < mBarBuffers.length; i++) {
            IBarDataSet set = barData.getDataSetByIndex(i);
            mBarBuffers[i] = new BarBuffer(set.getEntryCount() * 4 * (set.isStacked() ? set.getStackSize() : 1),
                    barData.getDataSetCount(), set.isStacked());
        }
    }

    @Override
    public void drawData(Canvas c) {

        BarData barData = mChart.getBarData();

        boolean hasValuesToHighlight = ((BarChart) mChart).valuesToHighlight();
        boolean isMakeUnhighlightedEntriesSmalledEnabled = mChart.isMakeUnhighlightedEntriesSmalledEnabled();
        boolean isDimmingEnabled = mChart.isDimmingEnabled();

        float decreaseScale = mChart.getDecreaseScaleForUnhighlightedEntry();
        int dimmingAlpha = mChart.getDimmingAlpha();

        float scale = isMakeUnhighlightedEntriesSmalledEnabled && hasValuesToHighlight ? decreaseScale : 1.0f;
        int alpha = isDimmingEnabled && hasValuesToHighlight ? dimmingAlpha : 255;

        for (int i = 0; i < barData.getDataSetCount(); i++) {

            IBarDataSet set = barData.getDataSetByIndex(i);

            if (set.isVisible()) {
                drawDataSet(c, set, i, scale, alpha);
            }
        }
    }

    // comment
    private RectF mBarShadowRectBuffer = new RectF();

    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index, float scale, int alpha) {

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        float barWidth = mChart.getBarData().getBarWidth() * scale;
        float barWidthHalf = barWidth / 2.0f;

        // draw the bar shadow before the values
        // TODO: add support for dimming and for width scaling
        if (mChart.isDrawBarShadowEnabled()) {
            mShadowPaint.setColor(dataSet.getBarShadowColor());

            float x;

            for (int i = 0, count = Math.min((int)(Math.ceil((float)(dataSet.getEntryCount()) * phaseX)), dataSet.getEntryCount());
                i < count;
                i++) {

                BarEntry e = dataSet.getEntryForIndex(i);

                x = e.getX();

                mBarShadowRectBuffer.left = x - barWidthHalf;
                mBarShadowRectBuffer.right = x + barWidthHalf;

                trans.rectValueToPixel(mBarShadowRectBuffer);

                if (!mViewPortHandler.isInBoundsLeft(mBarShadowRectBuffer.right))
                    continue;

                if (!mViewPortHandler.isInBoundsRight(mBarShadowRectBuffer.left))
                    break;

                mBarShadowRectBuffer.top = mViewPortHandler.contentTop();
                mBarShadowRectBuffer.bottom = mViewPortHandler.contentBottom();

                c.drawRect(mBarShadowRectBuffer, mShadowPaint);
            }
        }

        // initialize the buffer
        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(phaseX, phaseY);
        buffer.setDataSet(index);
        buffer.setInverted(mChart.isInverted(dataSet.getAxisDependency()));
        buffer.setBarWidth(barWidth);

        buffer.feed(dataSet);

        trans.pointValuesToPixel(buffer.buffer);

        drawBarBufferContent(c, dataSet, scale, alpha, buffer.buffer);
    }

    protected void drawBarBufferContent(Canvas c, IBarDataSet dataSet, float scale, int alpha, float[] buffer) {

        final boolean isSingleColor = dataSet.getColors().size() == 1;

        boolean isInverted = mChart.isInverted(dataSet.getAxisDependency());
        boolean isStacked = dataSet.isStacked();
        int stackSize = isStacked ? dataSet.getStackSize() : 1;

        float radius = dataSet.getCornerRadius();
        float minBarHeight = dataSet.getMinBarHeight();
        boolean isRoundedCornersEnabled = radius > 0.f;

        float barBorderWidth = dataSet.getBarBorderWidth();
        final boolean drawBorder = barBorderWidth > 0.f;

        mBarBorderPaint.setColor(dataSet.getBarBorderColor());
        mBarBorderPaint.setAlpha(alpha);
        mBarBorderPaint.setStrokeWidth(Utils.convertDpToPixel(barBorderWidth));

        if (isSingleColor) {
            mRenderPaint.setColor(dataSet.getColor());
            mRenderPaint.setAlpha(alpha);
        }


        // -------- WARNING ----------
        // @stackIndexCount is used here to allow easier detection of top rectangles (to make it rounded at the top).
        // This way of doing the feature assumes that a dataset have constant stack size for each bar (like 3, 3, 3).
        // The library allow a dataSet to contain different stack sizes (like 3, 1 ,2 ...), and the method getStackSize is
        //  **** Returns the _MAXIMUM_ number of bars that can be stacked upon another in this DataSet ****

        // So keep that in mind and if we ever need non constant stack size inside one dataSet, we will need to review
        // the appropriate approach here
        int stackIndexCount = 1;
        boolean isTopRect = false;

        for (int j = 0; j < buffer.length; j += 4) {

            isTopRect = false;
            
            // helps to find the top bar
            if(stackIndexCount < stackSize) {
                stackIndexCount++;
            } else {
                isTopRect = true;
                stackIndexCount = 1;
            }

            float left = buffer[j];
            float top = buffer[j + 1];
            float right = buffer[j + 2];
            float bottom = buffer[j + 3];

            float width = right - left;

            // Each corner receives two radius values [X, Y]. 
            // The corners are ordered top-left, top-right, bottom-right, bottom-left
            // 8 float values
            float[] radii = new float[]
            {
                radius, radius, // top-left
                radius, radius, // top-right
                0, 0, // bottom-right
                0, 0  // bottom-left
            };

            float[] radiuses = isRoundedCornersEnabled && isTopRect ? radii : radiiZeros;

            if (!mViewPortHandler.isInBoundsLeft(right))
                continue;

            if (!mViewPortHandler.isInBoundsRight(left))
                break;

            if (!isSingleColor) {
                // Set the color for the currently drawn value. If the index
                // is out of bounds, reuse colors.
                mRenderPaint.setColor(dataSet.getColor(j / 4));
                mRenderPaint.setAlpha(alpha);
            }

            if (dataSet.getGradientColor() != null) {
                GradientColor gradientColor = dataSet.getGradientColor();
                 mRenderPaint.setShader(
                    new LinearGradient(
                        left,
                        bottom,
                        left,
                        top,
                        gradientColor.getStartColor(),
                        gradientColor.getEndColor(),
                        android.graphics.Shader.TileMode.MIRROR));
            }

            if (dataSet.getGradientColors() != null) {
                 mRenderPaint.setShader(
                    new LinearGradient(
                        left,
                        bottom,
                        left,
                        top,
                        dataSet.getGradientColor(j / 4).getStartColor(),
                        dataSet.getGradientColor(j / 4).getEndColor(),
                        android.graphics.Shader.TileMode.MIRROR));
            }

            mRoundedRectsPath.reset();

            float[] clampedTopAndBottom = clampBarHeight(top, bottom, minBarHeight, isInverted);
            top = clampedTopAndBottom[0];
            bottom = clampedTopAndBottom[1];

            mRoundedRectsPath.addRoundRect(left, top, right, bottom, radiuses, Path.Direction.CW);
            
            c.drawPath(mRoundedRectsPath, mRenderPaint);

            if (drawBorder) {
                c.drawPath(mRoundedRectsPath, mBarBorderPaint);
            }
        }
    }

    // supports !inverted && y >= 0
    // TODO: need support for other cases...
    // returned array should contain new top at the first place  
    protected float[] clampBarHeight(float top, float bottom, float minBarHeight, boolean isInverted) {
        
        if(!isInverted && (top < bottom) && (bottom - top < minBarHeight)) {
            float newTop = bottom - minBarHeight;
            return new float[]{ newTop, bottom };
        }
        // return values unchanged
        return new float[]{ top, bottom };
    }


    // used to create rect for stack highlight mainly
    protected void prepareBarHighlight(float x, float y1, float y2, float barWidthHalf, Transformer trans) {

        float left = x - barWidthHalf;
        float right = x + barWidthHalf;
        float top = y1;
        float bottom = y2;

        mBarRect.set(left, top, right, bottom);

        trans.rectToPixelPhase(mBarRect, mAnimator.getPhaseY());
    }

    @Override
    public void drawValues(Canvas c) {

        // if values are drawn
        if (isDrawingValuesAllowed(mChart)) {

            List<IBarDataSet> dataSets = mChart.getBarData().getDataSets();

            final float valueOffsetPlus = Utils.convertDpToPixel(4.5f);
            float posOffset = 0f;
            float negOffset = 0f;
            boolean drawValueAboveBar = mChart.isDrawValueAboveBarEnabled();

            for (int i = 0; i < mChart.getBarData().getDataSetCount(); i++) {

                IBarDataSet dataSet = dataSets.get(i);

                if (!shouldDrawValues(dataSet))
                    continue;

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet);

                boolean isInverted = mChart.isInverted(dataSet.getAxisDependency());

                // calculate the correct offset depending on the draw position of
                // the value
                float valueTextHeight = Utils.calcTextHeight(mValuePaint, "8");
                posOffset = (drawValueAboveBar ? -valueOffsetPlus : valueTextHeight + valueOffsetPlus);
                negOffset = (drawValueAboveBar ? valueTextHeight + valueOffsetPlus : -valueOffsetPlus);

                if (isInverted) {
                    posOffset = -posOffset - valueTextHeight;
                    negOffset = -negOffset - valueTextHeight;
                }

                // get the buffer
                BarBuffer buffer = mBarBuffers[i];

                final float phaseY = mAnimator.getPhaseY();

                ValueFormatter formatter = dataSet.getValueFormatter();

                MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
                iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
                iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

                // if only single values are drawn (sum)
                if (!dataSet.isStacked()) {

                    for (int j = 0; j < buffer.buffer.length * mAnimator.getPhaseX(); j += 4) {

                        float x = (buffer.buffer[j] + buffer.buffer[j + 2]) / 2f;

                        if (!mViewPortHandler.isInBoundsRight(x))
                            break;

                        if (!mViewPortHandler.isInBoundsY(buffer.buffer[j + 1])
                                || !mViewPortHandler.isInBoundsLeft(x))
                            continue;

                        BarEntry entry = dataSet.getEntryForIndex(j / 4);
                        float val = entry.getY();

                        if (dataSet.isDrawValuesEnabled()) {
                            drawValue(c, formatter.getBarLabel(entry), x, val >= 0 ?
                                            (buffer.buffer[j + 1] + posOffset) :
                                            (buffer.buffer[j + 3] + negOffset),
                                    dataSet.getValueTextColor(j / 4));
                        }

                        if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                            Drawable icon = entry.getIcon();

                            float px = x;
                            float py = val >= 0 ?
                                    (buffer.buffer[j + 1] + posOffset) :
                                    (buffer.buffer[j + 3] + negOffset);

                            px += iconsOffset.x;
                            py += iconsOffset.y;

                            Utils.drawImage(
                                    c,
                                    icon,
                                    (int)px,
                                    (int)py,
                                    icon.getIntrinsicWidth(),
                                    icon.getIntrinsicHeight());
                        }
                    }

                    // if we have stacks
                } else {

                    Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

                    int bufferIndex = 0;
                    int index = 0;

                    while (index < dataSet.getEntryCount() * mAnimator.getPhaseX()) {

                        BarEntry entry = dataSet.getEntryForIndex(index);

                        float[] vals = entry.getYVals();
                        float x = (buffer.buffer[bufferIndex] + buffer.buffer[bufferIndex + 2]) / 2f;

                        int color = dataSet.getValueTextColor(index);

                        // we still draw stacked bars, but there is one
                        // non-stacked
                        // in between
                        if (vals == null) {

                            if (!mViewPortHandler.isInBoundsRight(x))
                                break;

                            if (!mViewPortHandler.isInBoundsY(buffer.buffer[bufferIndex + 1])
                                    || !mViewPortHandler.isInBoundsLeft(x))
                                continue;

                            if (dataSet.isDrawValuesEnabled()) {
                                drawValue(c, formatter.getBarLabel(entry), x, buffer.buffer[bufferIndex + 1] +
                                                (entry.getY() >= 0 ? posOffset : negOffset),
                                        color);
                            }

                            if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                                Drawable icon = entry.getIcon();

                                float px = x;
                                float py = buffer.buffer[bufferIndex + 1] +
                                        (entry.getY() >= 0 ? posOffset : negOffset);

                                px += iconsOffset.x;
                                py += iconsOffset.y;

                                Utils.drawImage(
                                        c,
                                        icon,
                                        (int)px,
                                        (int)py,
                                        icon.getIntrinsicWidth(),
                                        icon.getIntrinsicHeight());
                            }

                            // draw stack values
                        } else {

                            float[] transformed = new float[vals.length * 2];

                            float posY = 0f;
                            float negY = -entry.getNegativeSum();

                            for (int k = 0, idx = 0; k < transformed.length; k += 2, idx++) {

                                float value = vals[idx];
                                float y;

                                if (value == 0.0f && (posY == 0.0f || negY == 0.0f)) {
                                    // Take care of the situation of a 0.0 value, which overlaps a non-zero bar
                                    y = value;
                                } else if (value >= 0.0f) {
                                    posY += value;
                                    y = posY;
                                } else {
                                    y = negY;
                                    negY -= value;
                                }

                                transformed[k + 1] = y * phaseY;
                            }

                            trans.pointValuesToPixel(transformed);

                            for (int k = 0; k < transformed.length; k += 2) {

                                final float val = vals[k / 2];
                                final boolean drawBelow =
                                        (val == 0.0f && negY == 0.0f && posY > 0.0f) ||
                                                val < 0.0f;
                                float y = transformed[k + 1]
                                        + (drawBelow ? negOffset : posOffset);

                                if (!mViewPortHandler.isInBoundsRight(x))
                                    break;

                                if (!mViewPortHandler.isInBoundsY(y)
                                        || !mViewPortHandler.isInBoundsLeft(x))
                                    continue;

                                if (dataSet.isDrawValuesEnabled()) {
                                    drawValue(c, formatter.getBarStackedLabel(val, entry), x, y, color);
                                }

                                if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                                    Drawable icon = entry.getIcon();

                                    Utils.drawImage(
                                            c,
                                            icon,
                                            (int)(x + iconsOffset.x),
                                            (int)(y + iconsOffset.y),
                                            icon.getIntrinsicWidth(),
                                            icon.getIntrinsicHeight());
                                }
                            }
                        }

                        bufferIndex = vals == null ? bufferIndex + 4 : bufferIndex + 4 * vals.length;
                        index++;
                    }
                }

                MPPointF.recycleInstance(iconsOffset);
            }
        }
    }

    @Override
    public void drawValue(Canvas c, String valueText, float x, float y, int color) {
        mValuePaint.setColor(color);
        c.drawText(valueText, x, y, mValuePaint);
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {

        boolean isEnlargeEntryOnHighlightEnabled = mChart.isEnlargeEntryOnHighlightEnabled();
        float enlargementScale = mChart.getEnlargementScaleForHighlightedEntry();

        float scale = isEnlargeEntryOnHighlightEnabled ? enlargementScale : 1.0f;
        int alpha = 255;

        BarData barData = mChart.getBarData();
        float barWidth = barData.getBarWidth() * scale;
        float barWidthHalf = barWidth / 2.0f;

        float phaseX = mAnimator.getPhaseX();
        float phaseY = mAnimator.getPhaseY();

        for (Highlight high : indices) {

            int dataSetIndex = high.getDataSetIndex();

            IBarDataSet set = barData.getDataSetByIndex(dataSetIndex);

            if (set == null || !set.isHighlightEnabled())
                continue;

            BarEntry e = set.getEntryForXValue(high.getX(), high.getY());

            int entryIndex = set.getEntryIndex(e);

            if (!isInBoundsX(e, set))
                continue;

            Transformer trans = mChart.getTransformer(set.getAxisDependency());

            boolean isStack = (high.getStackIndex() >= 0  && e.isStacked()) ? true : false;

            final float y1;
            final float y2;

            if (isStack) {

                if(mChart.isHighlightFullBarEnabled()) {

                    y1 = e.getPositiveSum();
                    y2 = -e.getNegativeSum();

                } else {

                    Range range = e.getRanges()[high.getStackIndex()];

                    y1 = range.from;
                    y2 = range.to;
                }

            } else {
                y1 = e.getY();
                y2 = 0.f;
            }

            BarBuffer buffer = mBarBuffers[dataSetIndex];
            buffer.setPhases(phaseX, phaseY);
            buffer.setDataSet(dataSetIndex);
            buffer.setInverted(mChart.isInverted(set.getAxisDependency()));
            buffer.setBarWidth(barWidth);

            buffer.feed(set);

            float[] finalBuffer = buffer.getBufferByEntryIndex(entryIndex, set.isStacked() ? set.getStackSize() : 1);

            trans.pointValuesToPixel(finalBuffer);

            drawBarBufferContent(c, set, scale, alpha, finalBuffer);

            // TODO: saved for the stacked highlight / but arrow with stacked highlight haven't been tested
            prepareBarHighlight(e.getX(), y1, y2, barWidthHalf, trans);

            // TODO: should use y1 (strange name - rename) for highlight here
            if(set.getHighlightLineWidth() > 0.0) {
                MPPointD pix = trans.getPixelForValues(e.getX(), e.getY() * mAnimator
                    .getPhaseY());
                high.setDraw((float) pix.x, (float) pix.y);
                drawHighlightArrow(c, (float) pix.x, (float) pix.y, set, 2f);
            }
        }
    }

    /**
     * Sets the drawing position of the highlight object based on the riven bar-rect.
     * @param high
     */
    protected void setHighlightDrawPos(Highlight high, RectF bar) {
        high.setDraw(bar.centerX(), bar.top);
    }

    @Override
    public void drawExtras(Canvas c) {
    }
}
