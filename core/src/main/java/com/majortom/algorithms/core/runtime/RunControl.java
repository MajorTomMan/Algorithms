package com.majortom.algorithms.core.runtime;

/**
 * Bound cancellation and checkpoint control for one run.
 *
 * <p>Keeping both responsibilities on one object guarantees that cancellation can wake an
 * execution currently blocked at a checkpoint.</p>
 */
public interface RunControl extends CancellationToken, ExecutionGate {

    static RunControl unrestricted() {
        return new RunControl() {
            @Override
            public boolean isCancellationRequested() {
                return false;
            }

            @Override
            public void awaitPermission(CancellationToken cancellationToken) {
            }
        };
    }
}
