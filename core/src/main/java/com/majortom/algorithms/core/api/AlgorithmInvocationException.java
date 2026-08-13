package com.majortom.algorithms.core.api;

import java.util.Objects;

/** Deterministic failure raised at the erased invocation boundary. */
public final class AlgorithmInvocationException extends RuntimeException {

    private final String code;

    public AlgorithmInvocationException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
