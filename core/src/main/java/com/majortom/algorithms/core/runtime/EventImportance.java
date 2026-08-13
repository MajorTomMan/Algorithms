package com.majortom.algorithms.core.runtime;

/**
 * Retention and presentation importance assigned while reducing an execution event.
 */
public enum EventImportance {
    /** Short-lived presentation detail that an observer may coalesce under load. */
    TRANSIENT,
    /** A durable algorithm or view-state change. */
    STATE_CHANGE,
    /** A useful boundary for storing a replay checkpoint. */
    CHECKPOINT,
    /** A terminal execution boundary that must never be discarded. */
    TERMINAL
}
