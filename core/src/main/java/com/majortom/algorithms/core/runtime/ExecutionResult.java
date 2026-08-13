package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.api.AlgorithmOutput;

import java.util.Objects;
import java.util.Optional;

/** Immutable terminal result for the type-erased runtime boundary. */
public record ExecutionResult(
        ExecutionStatus status,
        Optional<AlgorithmOutput> output,
        Optional<ExecutionFailure> failure) {

    public ExecutionResult {
        Objects.requireNonNull(status, "status");
        output = Objects.requireNonNull(output, "output");
        failure = Objects.requireNonNull(failure, "failure");
        if (status == ExecutionStatus.COMPLETED && output.isEmpty()) {
            throw new IllegalArgumentException("A completed result requires an output");
        }
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

    public static ExecutionResult completed(AlgorithmOutput output) {
        return new ExecutionResult(ExecutionStatus.COMPLETED, Optional.of(output), Optional.empty());
    }

    public static ExecutionResult cancelled() {
        return new ExecutionResult(ExecutionStatus.CANCELLED, Optional.empty(), Optional.empty());
    }

    public static ExecutionResult failed(ExecutionFailure failure) {
        return new ExecutionResult(ExecutionStatus.FAILED, Optional.empty(), Optional.of(failure));
    }
}
