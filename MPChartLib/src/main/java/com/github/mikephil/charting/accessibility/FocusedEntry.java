package com.github.mikephil.charting.accessibility;

import com.github.mikephil.charting.data.Entry;

public class FocusedEntry {
    Entry entry;
    int dataSetIndex;

    public FocusedEntry(Entry entry, int dataSetIndex) {
        this.entry = entry;
        this.dataSetIndex = dataSetIndex;
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
}
