
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

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.github.mikephil.charting.accessibility.ExploreByTouchHelper;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.Utils;

import java.util.List;

/**
 * Chart that draws lines, surfaces, circles, ...
 *
 * @author Philipp Jahoda
 */
public class LineChart extends BarLineChartBase<LineData> implements LineDataProvider {

    /** Explore by touch helper, used to expose contents for accessibility. */
    private LineGraphAccessHelper mLineGraphAccessHelper;

    public LineChart(Context context) {
        super(context);
    }

    public LineChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();

        mRenderer = new LineChartRenderer(this, mAnimator, mViewPortHandler);

        if (mData != null) {
            mLineGraphAccessHelper = new LineGraphAccessHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mLineGraphAccessHelper);
        }
    }

    @Override
    public void setData(LineData data) {
        super.setData(data);

        if (mData != null) {
            mLineGraphAccessHelper = new LineGraphAccessHelper(this);
            ViewCompat.setAccessibilityDelegate(this, mLineGraphAccessHelper);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean dispatchHoverEvent(MotionEvent event) {
        // Always attempt to dispatch hover events to accessibility first.
        if ((mLineGraphAccessHelper != null)
                && mLineGraphAccessHelper.dispatchHoverEvent(event)) {
            return true;
        }

        return super.dispatchHoverEvent(event);
    }

    @Override
    public LineData getLineData() {
        return mData;
    }

    @Override
    protected void onDetachedFromWindow() {
        // releases the bitmap in the renderer to avoid oom error
        if (mRenderer != null && mRenderer instanceof LineChartRenderer) {
            ((LineChartRenderer) mRenderer).releaseBitmap();
        }
        super.onDetachedFromWindow();
    }

    public int getDataSetIndex(int index) {
        int size = index;
        for (int i = 0; i < mData.getDataSets().size(); i++) {
            if (size == 0) return i;
            int dataSetSize = ((LineDataSet)mData.getDataSets().get(i)).getValues().size();
            if (dataSetSize <= size) {
                size -= dataSetSize;
            } else {
                return i;
            }
        }
        return 0;
    }

    public int getEntryIndex(int dataSetIndex, int index) {
        int size = -1;
        if (dataSetIndex == 0) return index;
        for (int i = 0; i <= dataSetIndex; i++) {
            size += ((LineDataSet)mData.getDataSets().get(i)).getValues().size();
        }

        return size - index;
    }

    public Rect getLineBoundsByIndex(int index, Rect outputRect) {
        RectF bounds = new RectF(outputRect);

        if (mData == null) {
            bounds.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return new Rect(Math.round(bounds.left),Math.round(bounds.top), Math.round(bounds.right), Math.round(bounds.bottom));
        }

        int dataSetIndex = getDataSetIndex(index);
        ILineDataSet set = mData.getDataSetByIndex(dataSetIndex);
        Entry e = set.getEntryForIndex(getEntryIndex(dataSetIndex, index));
        float x = e.getX();

        float barWidthHalf = 0.2f / 2f;

        float left = x - barWidthHalf;
        float right = x + barWidthHalf;
        float top = mViewPortHandler.contentTop();
        float bottom = mViewPortHandler.contentBottom();

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(bounds);

        Rect rect = Utils.rectFtoRect(bounds);
        rect.top = (int) top;
        rect.bottom = (int) bottom;
        return rect;
    }

    /**
     * Implementation of {@link ExploreByTouchHelper} that exposes the contents
     * of a {@link BarChart} to accessibility services by mapping each bar
     * to a virtual view.
     */
    private class LineGraphAccessHelper extends ExploreByTouchHelper {
        private final Rect mTempParentBounds = new Rect();

        public LineGraphAccessHelper(View parentView) {
            super(parentView);
        }

        @Override
        protected int getVirtualViewIdAt(float x, float y) {
            // We already map (x,y) to bar index for onTouchEvent().
            if (mData != null) {
                int index = -1;
                for (int i = 0; i < mData.getDataSets().size(); i++) {
                    index = mData.getDataSetByIndex(i).getEntryIndex(x, y, DataSet.Rounding.DOWN);
                    if (index != -1) break;
                }
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

        private CharSequence getDescriptionForIndex(int index) {
            if (mData != null) {
                int dataSetIndex = getDataSetIndex(index);
                int entryIndex = getEntryIndex(dataSetIndex, index);
                Entry e = mData.getDataSetByIndex(dataSetIndex).getEntryForIndex(entryIndex);
                return e.getAccessibilityLabel();
            }
            return "accessibility label";
        }

        @Override
        protected void populateEventForVirtualViewId(int virtualViewId, AccessibilityEvent event) {
            final CharSequence desc = getDescriptionForIndex(virtualViewId);
            event.setContentDescription(desc);
        }

        @Override
        protected void populateNodeForVirtualViewId(
                int virtualViewId, AccessibilityNodeInfoCompat node) {
            // Node and event descriptions are usually identical.
            final CharSequence desc = getDescriptionForIndex(virtualViewId);
            node.setContentDescription(desc);

            // Since the user can tap a bar, add the CLICK action.
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);

            // Reported bounds should be consistent with onDraw().
            final Rect bounds = getLineBoundsByIndex(virtualViewId, mTempParentBounds);
            node.setBoundsInParent(bounds);
        }

        @Override
        protected boolean performActionForVirtualViewId(
                int virtualViewId, int action, Bundle arguments) {
            if (mData != null && virtualViewId > 0) {
                int dataSetIndex = getDataSetIndex(virtualViewId);
                int entryIndex = getEntryIndex(dataSetIndex, virtualViewId);
                Entry entry = mData.getDataSetByIndex(dataSetIndex).getEntryForIndex(entryIndex);
                switch (action) {
                    case AccessibilityNodeInfoCompat.ACTION_CLICK:
                        Highlight high = new Highlight(entry.getX(), entry.getY(), 0);
                        highlightValue(high);
                        mSelectionListener.onValueSelected(entry, high);
                        return true;
                }
            }
            return false;
        }
    }
}
