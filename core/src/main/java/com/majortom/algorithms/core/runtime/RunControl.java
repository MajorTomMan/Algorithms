package com.majortom.algorithms.core.runtime;

/** Bound pause/step/cancel control for one ordered run. */
public interface RunControl extends CancellationToken, ExecutionGate {

    /**
     * Waits until Runtime may enter the operation body.
     * A queued step permit may allow entry while paused, but is preserved for the first domain execution unit.
     */
    default void awaitStartPermission(CancellationToken cancellationToken) throws InterruptedException {
        awaitPermission(cancellationToken);
    }

    /** Waits until a Structure/Observation/checkpoint execution unit may proceed. */
    default void awaitDomainEventPermission(CancellationToken cancellationToken) throws InterruptedException {
        awaitPermission(cancellationToken);
    }

    /**
     * Waits until Runtime may publish terminal completion.
     * Step permits do not resume a paused run, so completion remains blocked until Resume or cancellation.
     */
    default void awaitCompletionPermission(CancellationToken cancellationToken) throws InterruptedException {
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
