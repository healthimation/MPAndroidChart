package com.github.mikephil.charting.charts;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.github.mikephil.charting.accessibility.ExploreByTouchHelper;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.BarHighlighter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;

import java.util.List;

import static com.github.mikephil.charting.utils.AccessibilityUtils.getAllDataSetsSortedByX;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getBarEntryIndexByX;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getDataSetIndex;
import static com.github.mikephil.charting.utils.AccessibilityUtils.rectFtoRect;

/**
 * Chart that draws bars.
 *
 * @author Philipp Jahoda
 */
public class BarChart extends BarLineChartBase<BarData> implements BarDataProvider {

    /** Explore by touch helper, used to expose contents for accessibility. */
    private BarGraphAccessHelper mBarGraphAccessHelper;

    /**
     * flag that indicates whether the highlight should be full-bar oriented, or single-value?
     */
    protected boolean mHighlightFullBarEnabled = false;

    /**
     * if set to true, all values are drawn above their bars, instead of below their top
     */
    private boolean mDrawValueAboveBar = true;

    /**
     * if set to true, a grey area is drawn behind each bar that indicates the maximum value
     */
    private boolean mDrawBarShadow = false;

    private boolean mFitBars = false;

    public BarChart(Context context) {
        super(context);
    }

    public BarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new BarChartRenderer(this, mAnimator, mViewPortHandler);

        setHighlighter(new BarHighlighter(this));

        getXAxis().setSpaceMin(0.5f);
        getXAxis().setSpaceMax(0.5f);
        if (mData != null) {
            mBarGraphAccessHelper = new BarGraphAccessHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mBarGraphAccessHelper);
        }
    }

    @Override
    public void setData(BarData data) {
        super.setData(data);

        if (mData != null) {
            mBarGraphAccessHelper = new BarGraphAccessHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mBarGraphAccessHelper);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Always attempt to dispatch hover events to accessibility first.
        if ((mBarGraphAccessHelper != null)
                && mBarGraphAccessHelper.dispatchHoverEvent(event)) {
            return true;
        }

        return super.dispatchHoverEvent(event);
    }

    @Override
    protected void calcMinMax() {

        if (mFitBars) {
            mXAxis.calculate(mData.getXMin() - mData.getBarWidth() / 2f, mData.getXMax() + mData.getBarWidth() / 2f);
        } else {
            mXAxis.calculate(mData.getXMin(), mData.getXMax());
        }

        // calculate axis range (min / max) according to provided data
        mAxisLeft.calculate(mData.getYMin(YAxis.AxisDependency.LEFT), mData.getYMax(YAxis.AxisDependency.LEFT));
        mAxisRight.calculate(mData.getYMin(YAxis.AxisDependency.RIGHT), mData.getYMax(YAxis.AxisDependency
                .RIGHT));
    }

    /**
     * Returns the Highlight object (contains x-index and DataSet index) of the selected value at the given touch
     * point
     * inside the BarChart.
     *
     * @param x
     * @param y
     * @return
     */
    @Override
    public Highlight getHighlightByTouchPoint(float x, float y) {

        if (mData == null) {
            Log.e(LOG_TAG, "Can't select by touch. No data set.");
            return null;
        } else {
            Highlight h = getHighlighter().getHighlight(x, y);
            if (h == null || !isHighlightFullBarEnabled()) return h;

            // For isHighlightFullBarEnabled, remove stackIndex
            return new Highlight(h.getX(), h.getY(),
                    h.getXPx(), h.getYPx(),
                    h.getDataSetIndex(), -1, h.getAxis());
        }
    }

    /**
     * Returns the bounding box of the specified Entry in the specified DataSet. Returns null if the Entry could not be
     * found in the charts data.  Performance-intensive code should use void getBarBounds(BarEntry, RectF) instead.
     *
     * @param e
     * @return
     */
    public RectF getBarBounds(BarEntry e) {

        RectF bounds = new RectF();
        getBarBounds(e, bounds);

        return bounds;
    }

    /**
     * The passed outputRect will be assigned the values of the bounding box of the specified Entry in the specified DataSet.
     * The rect will be assigned Float.MIN_VALUE in all locations if the Entry could not be found in the charts data.
     *
     * @param e
     * @return
     */
    public void getBarBounds(BarEntry e, RectF outputRect) {

        RectF bounds = outputRect;

        IBarDataSet set = mData.getDataSetForEntry(e);

        if (set == null) {
            bounds.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return;
        }

        float y = e.getY();
        float x = e.getX();

        float barWidth = mData.getBarWidth();

        float left = x - barWidth / 2f;
        float right = x + barWidth / 2f;
        float top = y >= 0 ? y : 0;
        float bottom = y <= 0 ? y : 0;

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(outputRect);
    }

    public Rect getBarBoundsByIndex(int index, Rect outputRect) {
        RectF bounds = new RectF(outputRect);

        if (mData == null) {
            bounds.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return new Rect(Math.round(bounds.left),Math.round(bounds.top), Math.round(bounds.right), Math.round(bounds.bottom));
        }

        int dataSetIndex = getDataSetIndex(index, mData);
        IBarDataSet set = mData.getDataSetByIndex(dataSetIndex);
        List<BarEntry> entries = getAllDataSetsSortedByX(mData);
        BarEntry e = entries.get(index);
        float x = e.getX();

        float barWidthHalf = mData.getBarWidth() / 2f;

        float left = x - barWidthHalf;
        float right = x + barWidthHalf;
        float top = mViewPortHandler.contentTop();
        float bottom = mViewPortHandler.contentBottom();

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(bounds);

        Rect rect = rectFtoRect(bounds);
        rect.top = (int) top;
        rect.bottom = (int) bottom;
        return rect;
    }

    /**
     * If set to true, all values are drawn above their bars, instead of below their top.
     *
     * @param enabled
     */
    public void setDrawValueAboveBar(boolean enabled) {
        mDrawValueAboveBar = enabled;
    }

    /**
     * returns true if drawing values above bars is enabled, false if not
     *
     * @return
     */
    public boolean isDrawValueAboveBarEnabled() {
        return mDrawValueAboveBar;
    }

    /**
     * If set to true, a grey area is drawn behind each bar that indicates the maximum value. Enabling his will reduce
     * performance by about 50%.
     *
     * @param enabled
     */
    public void setDrawBarShadow(boolean enabled) {
        mDrawBarShadow = enabled;
    }

    /**
     * returns true if drawing shadows (maxvalue) for each bar is enabled, false if not
     *
     * @return
     */
    public boolean isDrawBarShadowEnabled() {
        return mDrawBarShadow;
    }

    /**
     * Set this to true to make the highlight operation full-bar oriented, false to make it highlight single values (relevant
     * only for stacked). If enabled, highlighting operations will highlight the whole bar, even if only a single stack entry
     * was tapped.
     * Default: false
     *
     * @param enabled
     */
    public void setHighlightFullBarEnabled(boolean enabled) {
        mHighlightFullBarEnabled = enabled;
    }

    /**
     * @return true the highlight operation is be full-bar oriented, false if single-value
     */
    @Override
    public boolean isHighlightFullBarEnabled() {
        return mHighlightFullBarEnabled;
    }

    /**
     * Highlights the value at the given x-value in the given DataSet. Provide
     * -1 as the dataSetIndex to undo all highlighting.
     *
     * @param x
     * @param dataSetIndex
     * @param stackIndex   the index inside the stack - only relevant for stacked entries
     */
    public void highlightValue(float x, int dataSetIndex, int stackIndex) {
        highlightValue(new Highlight(x, dataSetIndex, stackIndex), false);
    }

    @Override
    public BarData getBarData() {
        return mData;
    }

    /**
     * Adds half of the bar width to each side of the x-axis range in order to allow the bars of the barchart to be
     * fully displayed.
     * Default: false
     *
     * @param enabled
     */
    public void setFitBars(boolean enabled) {
        mFitBars = enabled;
    }

    /**
     * Groups all BarDataSet objects this data object holds together by modifying the x-value of their entries.
     * Previously set x-values of entries will be overwritten. Leaves space between bars and groups as specified
     * by the parameters.
     * Calls notifyDataSetChanged() afterwards.
     *
     * @param fromX      the starting point on the x-axis where the grouping should begin
     * @param groupSpace the space between groups of bars in values (not pixels) e.g. 0.8f for bar width 1f
     * @param barSpace   the space between individual bars in values (not pixels) e.g. 0.1f for bar width 1f
     */
    public void groupBars(float fromX, float groupSpace, float barSpace) {

        if (getBarData() == null) {
            throw new RuntimeException("You need to set data for the chart before grouping bars.");
        } else {
            getBarData().groupBars(fromX, groupSpace, barSpace);
            notifyDataSetChanged();
        }
    }

    /**
     * Implementation of {@link ExploreByTouchHelper} that exposes the contents
     * of a {@link BarChart} to accessibility services by mapping each bar
     * to a virtual view.
     */
    private class BarGraphAccessHelper extends ExploreByTouchHelper {
        private final Rect mTempParentBounds = new Rect();

        public BarGraphAccessHelper(View parentView) {
            super(parentView);
        }

        @Override
        protected int getVirtualViewIdAt(float x, float y) {
            // We already map (x,y) to bar index for onTouchEvent().
            if (mData != null) {
                List<BarEntry> entries = getAllDataSetsSortedByX(mData);
                int index = getBarEntryIndexByX(entries, x);
                if (index >= 0) {
                    return index;
                }
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViewIds(List<Integer> virtualViewIds) {
            if (mData != null) {
                int count = mData.getEntryCount();
                for (int index = count - 1; index >= 0; index--) {
                    virtualViewIds.add(index);
                }
            }

        }

        @Override
        protected CharSequence getDescriptionForIndex(int index, AccessibilityNodeInfoCompat node) {
            if (mData != null) {
                List<BarEntry> entries = getAllDataSetsSortedByX(mData);
                BarEntry e = entries.get(index);
                node.setSelected(valuesToHighlight() && getHighlighted()[0].getX() == e.getX());
                return e.getAccessibilityLabel();
            }
            return "accessibility label";
        }

        @Override
        protected void populateEventForVirtualViewId(int virtualViewId, AccessibilityEvent event) {
            final CharSequence desc = getDescriptionForIndex(virtualViewId, AccessibilityNodeInfoCompat.obtain(getRootView(), virtualViewId));
            event.setContentDescription(desc);
        }

        @Override
        protected void populateNodeForVirtualViewId(
                int virtualViewId, AccessibilityNodeInfoCompat node) {
            // Node and event descriptions are usually identical.
            final CharSequence desc = getDescriptionForIndex(virtualViewId, node);
            node.setContentDescription(desc);

            // Since the user can tap a bar, add the CLICK action.
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

            // Reported bounds should be consistent with onDraw().
            final Rect bounds = getBarBoundsByIndex(virtualViewId, mTempParentBounds);
            node.setBoundsInParent(bounds);
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected boolean performActionForVirtualViewId(int virtualViewId, int action, Bundle arguments) {
            if (mData != null && virtualViewId > 0) return false;

            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    accessibilityClickAction(virtualViewId,
                            getAccessibilityLabelForVirtualViewId(getRootView(), virtualViewId),
                            false
                    );
                    return true;
                case AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS:
                    accessibilityPerformActions.clearAccessibilityFocus(virtualViewId, mData.getEntryCount());
                    return true;
            }
            return false;
        }
    }
}
