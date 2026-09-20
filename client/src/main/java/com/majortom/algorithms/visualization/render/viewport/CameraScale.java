package com.majortom.algorithms.visualization.render.viewport;

/** Shared scale constraints; automatic fit and manual zoom retain distinct upper bounds. */
public final class CameraScale {
    public static final double MIN = 0.10d;
    public static final double DEFAULT = 1.0d;
    public static final double MAX_AUTO_FIT = 1.35d;
    public static final double MAX_MANUAL = 8.0d;
    public static final double TOOLBAR_FACTOR = 1.15d;

    private CameraScale() {}
}
