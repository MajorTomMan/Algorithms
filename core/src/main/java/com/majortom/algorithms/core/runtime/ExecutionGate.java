package com.majortom.algorithms.core.runtime;

/** Controls whether an algorithm may pass an execution checkpoint. */
@FunctionalInterface
public interface ExecutionGate {

    void awaitPermission(CancellationToken cancellationToken) throws InterruptedException;

    static ExecutionGate open() {
        return cancellationToken -> {
        };
    }
}
