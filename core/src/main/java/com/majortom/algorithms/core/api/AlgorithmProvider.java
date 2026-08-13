package com.majortom.algorithms.core.api;

/** Supplies a fresh typed algorithm and its dynamic-boundary type information. */
public interface AlgorithmProvider<I extends AlgorithmInput, O extends AlgorithmOutput> {

    AlgorithmMetadata metadata();

    Class<I> inputType();

    Class<O> outputType();

    Algorithm<I, O> createAlgorithm();

    default InputValidator<I> inputValidator() {
        return InputValidator.acceptingAll();
    }

    default AlgorithmInvoker invoker() {
        return new TypedAlgorithmInvoker<>(this);
    }
}
