package com.github.mikephil.charting.utils;

import android.graphics.Rect;
import android.graphics.RectF;

import com.github.mikephil.charting.accessibility.FocusedEntry;
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleDataSet;
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AccessibilityUtils {
    public static Rect rectFtoRect(RectF rectF) {
        Rect rect = new Rect();
        rect.left = (int) rectF.left;
        rect.right = (int) rectF.right;
        rect.top = (int) rectF.top;
        rect.bottom = (int) rectF.bottom;
        return rect;
    }

    public static int getDataSetIndex(int index, ChartData mData) {
        int size = index;
        for (int i = 0; i < mData.getDataSets().size(); i++) {
            if (size == 0) return i;
            int dataSetSize = ((BarLineScatterCandleBubbleDataSet)mData.getDataSets().get(i)).getValues().size();
            if (dataSetSize <= size) {
                size -= dataSetSize;
            } else {
                return i;
            }
        }
        return 0;
    }

    public static int getEntryIndex(int dataSetIndex, int index, ChartData mData) {
        int size = -1;
        if (dataSetIndex == 0) return index;
        for (int i = 0; i <= dataSetIndex; i++) {
            size += ((BarLineScatterCandleBubbleDataSet)mData.getDataSets().get(i)).getValues().size();
        }

        return size - index;
    }

    public static Set<Float> countOfDataSetsByX(ChartData mData) {
        Set<Float> countOfDataSetsByX = new HashSet<>();
        for (Object dataSet : mData.getDataSets()) {
            float x = ((IDataSet)dataSet).getEntryForIndex(0).getX();
            countOfDataSetsByX.add(x);
        }
        return countOfDataSetsByX;
    }

    public static List<IDataSet> getDateSetsByX(float x, ChartData mData) {
        List<IDataSet> sets = new ArrayList<>();
        for(Object set : mData.getDataSets()) {
            if(((IDataSet)set).containsEntriesAtXValue(x, x)) {
                sets.add(((IDataSet)set));
            }
        }
        return sets;
    }

    public static float getMaxYFromDataSets(List<IDataSet> list) {
        float max = -1;
        for (IDataSet set: list) {
            if (max < set.getYMax()) max = set.getYMax();
        }
        return max;
    }

    public static float getMinYFromDataSets(List<IDataSet> list) {
        float min = Float.MAX_VALUE;
        for (IDataSet set: list) {
            if (min > set.getYMin()) min = set.getYMin();
        }
        return min;
    }

    public static FocusedEntry getFocusedEntry(boolean mGroupSelectionEnabled, ChartData mData, int viewIndex) {
        Entry entry = null;
        int dataSetIndex = 0;
        ILineDataSet set = null;
        if (mGroupSelectionEnabled) {
            Object[] countOfDataSetsByX = countOfDataSetsByX(mData).toArray();
            float x = (float) (countOfDataSetsByX.length <= viewIndex ? countOfDataSetsByX[0] : countOfDataSetsByX[viewIndex]);
            for(int i=0; i< mData.getDataSets().size(); i++) {
                set = (ILineDataSet) mData.getDataSets().get(i);
                if(set.containsEntriesAtXValue(x, x)) {
                    entry = set.getEntryForIndex(0);
                    dataSetIndex = i;
                    break;
                }
            }
        } else {
            dataSetIndex = getDataSetIndex(viewIndex, mData);
            int entryIndex = getEntryIndex(dataSetIndex, viewIndex, mData);
            entry = mData.getDataSetByIndex(dataSetIndex).getEntryForIndex(entryIndex);
        }
        return new FocusedEntry(entry, dataSetIndex, set);
    }
}
