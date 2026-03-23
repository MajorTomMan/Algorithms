package com.majortom.algorithms.core.runtime;

public record ExecutionMessage(
        long timestampMillis,
        ExecutionMessageLevel level,
        String code,
        String text) {
}
