
package com.github.mikephil.charting.charts;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.github.mikephil.charting.R;
import com.github.mikephil.charting.accessibility.ExploreByTouchHelper;
import com.github.mikephil.charting.accessibility.FocusedEntry;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.MPPointD;

import java.util.ArrayList;
import java.util.List;

import static com.github.mikephil.charting.utils.AccessibilityUtils.countOfDataSetsByX;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getDataSetIndex;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getDateSetsByX;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getEntryIndex;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getFocusedEntry;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getMaxYFromDataSets;
import static com.github.mikephil.charting.utils.AccessibilityUtils.getMinYFromDataSets;
import static com.github.mikephil.charting.utils.AccessibilityUtils.rectFtoRect;

/**
 * Chart that draws lines, surfaces, circles, ...
 *
 * @author Philipp Jahoda
 */
public class LineChart extends BarLineChartBase<LineData> implements LineDataProvider {

    private int entryCount = -1;

    /**
     * flag that indicates if group selection is on
     */
    protected boolean mGroupSelectionEnabled = false;

    public void setGroupSelectionEnabled(boolean enabled) {
        mGroupSelectionEnabled = enabled;
    }

    public boolean isGroupSelectionEnabled() {
        return mGroupSelectionEnabled;
    }
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
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void setData(LineData data) {
        super.setData(data);
        entryCount = -1;

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

    private MPPointD transformer(float x, float y, ILineDataSet set) {
        return getTransformer(set.getAxisDependency()).getPixelForValues(x, y * mAnimator.getPhaseY());
    }

    public Rect getLineBoundsByIndex(int index, Rect outputRect) {
        RectF bounds = new RectF(outputRect);
        if (mData == null) {
            bounds.set(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
            return new Rect(Math.round(bounds.left),Math.round(bounds.top), Math.round(bounds.right), Math.round(bounds.bottom));
        }
        Entry e;
        float x;
        ILineDataSet set = null;
        MPPointD pix = null;
        if (mGroupSelectionEnabled) {
            Object[] countOfDataSetsByX = countOfDataSetsByX(mData).toArray();
            x = (float) (countOfDataSetsByX.length <= index ? countOfDataSetsByX[0] : countOfDataSetsByX[index]);
            for(ILineDataSet dataSet : mData.getDataSets()) {
                if(dataSet.containsEntriesAtXValue(x, x)) {
                    set = dataSet;
                    break;
                }
            }
        } else {
            int dataSetIndex = getDataSetIndex(index, mData);
            set = mData.getDataSetByIndex(dataSetIndex);
            e = set.getEntryForIndex(getEntryIndex(dataSetIndex, index, mData));
            x = e.getX();
            pix = transformer(e.getX(), e.getY(), set);
        }
        float barWidthHalf = 1f / 2f;

        List<IDataSet> dataSetsWithDataBetween = getDateSetsByX(x, mData);
        float minGroupY = (float) transformer(x, getMinYFromDataSets(dataSetsWithDataBetween), set).y;
        float maxGroupY = (float) transformer(x, getMaxYFromDataSets(dataSetsWithDataBetween), set).y;

        float left = x - barWidthHalf;
        float right = x + barWidthHalf;
        float top = mGroupSelectionEnabled ? maxGroupY - 30f : (float) pix.y - 30f;
        float bottom = mGroupSelectionEnabled ? minGroupY + 30f  : (float) pix.y + 30f;

        bounds.set(left, top, right, bottom);

        getTransformer(set.getAxisDependency()).rectValueToPixel(bounds);

        Rect rect = rectFtoRect(bounds);
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
                    if (mGroupSelectionEnabled) {
                        List<ILineDataSet> dataSetsWithDataBetween = new ArrayList<>();
                        if (mData.getDataSetByIndex(i).containsEntriesAtXValue(x, x)) {
                            dataSetsWithDataBetween.add(mData.getDataSetByIndex(i));
                            continue;
                        } else {
                            index = i - dataSetsWithDataBetween.size() - 1;
                        }
                    } else {
                        index = mData.getDataSetByIndex(i).getEntryIndex(x, y, DataSet.Rounding.DOWN);
                    }
                    if (index != -1) break;
                }
                if (index >= 0) {
                    return index;
                }
            }
            return ExploreByTouchHelper.INVALID_ID;
        }

        private void calculateEntryCount() {
            if (mData != null) {
                if (mGroupSelectionEnabled) {
                    entryCount = countOfDataSetsByX(mData).size();
                } else {
                    entryCount = mData.getEntryCount();
                }
            }
        }

        @Override
        protected void getVisibleVirtualViewIds(List<Integer> virtualViewIds) {
            if (entryCount == -1) {
                calculateEntryCount();
            }
            for (int index = entryCount - 1; index >= 0; index--) {
                virtualViewIds.add(index);
            }
        }

        private CharSequence getDescriptionForIndex(int index, AccessibilityNodeInfoCompat node) {
            if (mData != null) {
                FocusedEntry focusedEntry = getFocusedEntry(mGroupSelectionEnabled, mData, index);
                Entry e = focusedEntry.getEntry();
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
            final Rect bounds = getLineBoundsByIndex(virtualViewId, mTempParentBounds);
            node.setBoundsInParent(bounds);
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected boolean performActionForVirtualViewId(
                int virtualViewId, int action, Bundle arguments) {
            if (mData != null && virtualViewId > 0) {
                switch (action) {
                    case AccessibilityNodeInfoCompat.ACTION_CLICK:
                        FocusedEntry focusedEntry = getFocusedEntry(mGroupSelectionEnabled, mData, virtualViewId);
                        Entry entry = focusedEntry.getEntry();
                        Highlight high = new Highlight(entry.getX(), entry.getY(), focusedEntry.getDataSetIndex());
                        highlightValue(high);
                        mSelectionListener.onValueSelected(entry, high);
                        AccessibilityNodeInfoCompat node = AccessibilityNodeInfoCompat.obtain(getRootView(), virtualViewId);
                        CharSequence accessibilityLabel = getDescriptionForIndex(virtualViewId, node);
                        getRootView().announceForAccessibility(getContext().getText(R.string.selected)+ ", " + accessibilityLabel);
                        return true;
                    case AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS:
                        accessibilityPerformActions.clearAccessibilityFocus(virtualViewId, mData.getEntryCount());
                        return true;
                }
            }
            return false;
        }
    }
}
