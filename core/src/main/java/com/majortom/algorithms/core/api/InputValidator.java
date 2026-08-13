package com.majortom.algorithms.core.api;

/** Validates invariants that cannot be checked through an erased {@link Class}. */
@FunctionalInterface
public interface InputValidator<I extends AlgorithmInput> {

    void validate(I input);

    static <I extends AlgorithmInput> InputValidator<I> acceptingAll() {
        return input -> {
        };
    }
}
