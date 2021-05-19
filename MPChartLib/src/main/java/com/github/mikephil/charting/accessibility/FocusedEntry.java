package com.github.mikephil.charting.accessibility;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class FocusedEntry {
    Entry entry;
    int dataSetIndex;
    ILineDataSet set;

    public FocusedEntry(Entry entry, int dataSetIndex) {
        this.entry = entry;
        this.dataSetIndex = dataSetIndex;
    }

    public FocusedEntry(Entry entry, int dataSetIndex, ILineDataSet set) {
        this.entry = entry;
        this.dataSetIndex = dataSetIndex;
        this.set = set;
    }

    public Entry getEntry() {
        return entry;
    }

    public void setEntry(Entry entry) {
        this.entry = entry;
    }

    public int getDataSetIndex() {
        return dataSetIndex;
    }

    public void setDataSetIndex(int dataSetIndex) {
        this.dataSetIndex = dataSetIndex;
    }

    public ILineDataSet getSet() {
        return set;
    }

    public void setSet(ILineDataSet set) {
        this.set = set;
    }
}
