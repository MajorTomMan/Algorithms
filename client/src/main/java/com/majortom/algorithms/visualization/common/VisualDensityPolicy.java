package com.majortom.algorithms.visualization.common;

/** One density decision shared by detached geometry and the corresponding FX views. */
public final class VisualDensityPolicy {
    private VisualDensityPolicy() {}

    public static VisualDensity array(int size) {
        if (size <= 16) return VisualDensity.DETAIL;
        if (size <= 40) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }

    public static VisualDensity string(int length) {
        if (length <= 24) return VisualDensity.DETAIL;
        if (length <= 48) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }
}
