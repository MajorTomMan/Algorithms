package com.majortom.algorithms.core.runtime;

import java.util.Objects;

/** Stable failure details without retaining an arbitrary exception object. */
public record ExecutionFailure(String code, String message, String exceptionType) {

    public ExecutionFailure {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(exceptionType, "exceptionType");
    }
}
