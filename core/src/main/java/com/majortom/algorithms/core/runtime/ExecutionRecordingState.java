package com.majortom.algorithms.core.runtime;

/** Lifecycle state represented by an execution recording snapshot. */
public enum ExecutionRecordingState {
    NOT_STARTED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED;

    /** Returns whether no further events may be appended to the recording. */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }
}
