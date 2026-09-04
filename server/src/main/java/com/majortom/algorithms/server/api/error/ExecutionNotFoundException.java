package com.majortom.algorithms.server.api.error;

public final class ExecutionNotFoundException extends RuntimeException {
    public ExecutionNotFoundException(String runId) {
        super("No execution found for runId: " + runId);
    }
}
