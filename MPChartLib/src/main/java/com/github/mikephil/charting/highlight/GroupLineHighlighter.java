package com.github.mikephil.charting.highlight;

import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.utils.MPPointD;

public class GroupLineHighlighter extends ChartHighlighter<LineDataProvider> {

    public GroupLineHighlighter(LineDataProvider chart) {
        super(chart);
    }

    @Override
    protected float getDistance(float x1, float y1, float x2, float y2) {
        return Math.abs(x1 - x2);
    }

    @Override
    protected BarLineScatterCandleBubbleData getData() {
        return mChart.getLineData();
    }
}
