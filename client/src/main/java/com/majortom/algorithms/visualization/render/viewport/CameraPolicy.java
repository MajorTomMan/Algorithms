package com.majortom.algorithms.visualization.render.viewport;

public enum CameraPolicy {
    KEEP,
    FIT_CONTENT,
    /** Auto-fit only when content fits at its default readable scale. */
    FIT_IF_READABLE,
    /** Restore a valid camera or auto-fit without shrinking below the default scale. */
    RESTORE_OR_FIT_IF_READABLE,
    /** Ensure visibility without shrinking a long strip below its default scale. */
    ENSURE_VISIBLE_IF_READABLE,
    CENTER,
    RESET,
    ENSURE_VISIBLE,
    RESTORE
}
