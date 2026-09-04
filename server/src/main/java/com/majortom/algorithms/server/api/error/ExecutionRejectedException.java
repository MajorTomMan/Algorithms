package com.majortom.algorithms.server.api.error;

public final class ExecutionRejectedException extends RuntimeException {
    private final String runId;

    public ExecutionRejectedException(String runId, Throwable cause) {
        super("Execution scheduler queue is full; rejected runId: " + runId, cause);
        this.runId = runId;
    }

    public String runId() {
        return runId;
    }
}
