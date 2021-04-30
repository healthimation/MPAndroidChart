package com.github.mikephil.charting.interfaces.dataprovider;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;

public interface LineDataProvider extends BarLineChartInterface {

    LineData getLineData();
    boolean isGroupSelectionEnabled();

    YAxis getAxis(YAxis.AxisDependency dependency);
}
