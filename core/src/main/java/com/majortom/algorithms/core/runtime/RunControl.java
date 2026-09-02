package com.majortom.algorithms.core.runtime;

/** Bound pause/step/cancel control for one ordered run. */
public interface RunControl extends CancellationToken, ExecutionGate {

    /** Waits until a domain event may be published. A paused run may consume one step permit. */
    default void awaitDomainEventPermission(CancellationToken cancellationToken) throws InterruptedException {
        awaitPermission(cancellationToken);
    }

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
