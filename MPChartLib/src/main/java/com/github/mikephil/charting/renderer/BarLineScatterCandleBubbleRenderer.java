package com.github.mikephil.charting.renderer;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.LinearGradient;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.dataprovider.BarLineScatterCandleBubbleDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * Created by Philipp Jahoda on 09/06/16.
 */
public abstract class BarLineScatterCandleBubbleRenderer extends DataRenderer {

    /**
     * buffer for storing the current minimum and maximum visible x
     */
    protected XBounds mXBounds = new XBounds();

    public BarLineScatterCandleBubbleRenderer(ChartAnimator animator, ViewPortHandler viewPortHandler) {
        super(animator, viewPortHandler);
    }

    /**
     * Returns true if the DataSet values should be drawn, false if not.
     *
     * @param set
     * @return
     */
    protected boolean shouldDrawValues(IDataSet set) {
        return set.isVisible() && (set.isDrawValuesEnabled() || set.isDrawIconsEnabled());
    }

    /**
     * Checks if the provided entry object is in bounds for drawing considering the current animation phase.
     *
     * @param e
     * @param set
     * @return
     */
    protected boolean isInBoundsX(Entry e, IBarLineScatterCandleBubbleDataSet set) {

        if (e == null)
            return false;

        float entryIndex = set.getEntryIndex(e);

        if (e == null || entryIndex >= set.getEntryCount() * mAnimator.getPhaseX()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Class representing the bounds of the current viewport in terms of indices in the values array of a DataSet.
     */
    protected class XBounds {

        /**
         * minimum visible entry index
         */
        public int min;

        /**
         * maximum visible entry index
         */
        public int max;

        /**
         * range of visible entry indices
         */
        public int range;

        /**
         * Calculates the minimum and maximum x values as well as the range between them.
         *
         * @param chart
         * @param dataSet
         */
        public void set(BarLineScatterCandleBubbleDataProvider chart, IBarLineScatterCandleBubbleDataSet dataSet) {
            float phaseX = Math.max(0.f, Math.min(1.f, mAnimator.getPhaseX()));

            float low = chart.getLowestVisibleX();
            float high = chart.getHighestVisibleX();

            Entry entryFrom = dataSet.getEntryForXValue(low, Float.NaN, DataSet.Rounding.DOWN);
            Entry entryTo = dataSet.getEntryForXValue(high, Float.NaN, DataSet.Rounding.UP);

            min = entryFrom == null ? 0 : dataSet.getEntryIndex(entryFrom);
            max = entryTo == null ? 0 : dataSet.getEntryIndex(entryTo);
            range = (int) ((max - min) * phaseX);
        }
    }

    // WARNING: tested only with linecharts / bar charts
    // Probably need some efforts to be able to work on other charts
    protected void drawHighlightArrow(Canvas c, float x, float y, IBarLineScatterCandleBubbleDataSet set, float insetBottom) {

        // 45 degree angle
        float strokeWidth = set.getHighlightLineWidth();
        int color = set.getHighLightColor();
        float basicHeight = (float) (strokeWidth / Math.sqrt(2));

        mHighlightPaint.setColor(color);
        mHighlightPaint.setStrokeWidth(strokeWidth);

        float topPointOfArrowHead = mViewPortHandler.contentTop();
        float bottomPointOfArrow = y - insetBottom;
        float topPointOfArrow = topPointOfArrowHead + basicHeight;
        // float arrowHeight = bottomPointOfArrow - topPointOfArrow;

        float arrowHeadHeight = 4f * basicHeight;
        float arrowHeadHalfWidth = 3f * basicHeight;

        int[] colors = new int[]{color, Color.TRANSPARENT};
        float[] positions = new float[]{0.6f, 1f};
        Shader shader = new LinearGradient(0, topPointOfArrow, 0, bottomPointOfArrow, colors, positions, Shader.TileMode.MIRROR);

        // ACTUALLY DRAWING THE ARROW
        mHighlightPaint.setShader(shader);

        c.drawLine(x, topPointOfArrow, x, bottomPointOfArrow, mHighlightPaint);

        mHighlightPaint.setShader(null);

        c.drawLine(x - arrowHeadHalfWidth, topPointOfArrowHead + arrowHeadHeight, x + basicHeight/2, topPointOfArrowHead + basicHeight/2, mHighlightPaint);
        c.drawLine(x - basicHeight/2, topPointOfArrowHead + basicHeight/2, x + arrowHeadHalfWidth, topPointOfArrowHead + arrowHeadHeight, mHighlightPaint);
    
    }
}
