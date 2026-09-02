package com.majortom.algorithms.core.runtime;

import java.util.Objects;
import java.util.Optional;

/** Immutable terminal result for one runtime-managed operation. */
public record ExecutionResult(
        ExecutionStatus status,
        Optional<Object> output,
        Optional<ExecutionFailure> failure) {

    public ExecutionResult {
        Objects.requireNonNull(status, "status");
        output = Objects.requireNonNull(output, "output");
        failure = Objects.requireNonNull(failure, "failure");
        if (status != ExecutionStatus.COMPLETED && output.isPresent()) {
            throw new IllegalArgumentException("Only a completed result may contain an output");
        }
        if (status == ExecutionStatus.FAILED && failure.isEmpty()) {
            throw new IllegalArgumentException("A failed result requires failure details");
        }
        if (status != ExecutionStatus.FAILED && failure.isPresent()) {
            throw new IllegalArgumentException("Only a failed result may contain failure details");
        }
    }

    public static ExecutionResult completed(Object output) {
        return new ExecutionResult(ExecutionStatus.COMPLETED, Optional.ofNullable(output), Optional.empty());
    }

    public static ExecutionResult completed() {
        return completed(null);
    }

    public static ExecutionResult cancelled() {
        return new ExecutionResult(ExecutionStatus.CANCELLED, Optional.empty(), Optional.empty());
    }

    public static ExecutionResult failed(ExecutionFailure failure) {
        return new ExecutionResult(ExecutionStatus.FAILED, Optional.empty(), Optional.of(failure));
    }
}
