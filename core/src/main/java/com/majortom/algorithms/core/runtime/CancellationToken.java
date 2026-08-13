package com.majortom.algorithms.core.runtime;

/** Read-only cooperative cancellation port. */
@FunctionalInterface
public interface CancellationToken {

    boolean isCancellationRequested();

    static CancellationToken none() {
        return () -> false;
    }
}
