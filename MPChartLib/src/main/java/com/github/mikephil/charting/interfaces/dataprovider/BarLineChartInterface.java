package com.github.mikephil.charting.interfaces.dataprovider;

public interface BarLineChartInterface extends BarLineScatterCandleBubbleDataProvider {
    boolean isEnlargeEntryOnHighlightEnabled();
    boolean isMakeUnhighlightedEntriesSmalledEnabled();
    boolean isDimmingEnabled();
    
    float getEnlargementScaleForHighlightedEntry();
    float getDecreaseScaleForUnhighlightedEntry();
    int getDimmingAlpha();
}
