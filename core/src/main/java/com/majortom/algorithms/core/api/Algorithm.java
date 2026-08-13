package com.majortom.algorithms.core.api;

/** A strongly typed, UI-neutral algorithm. */
@FunctionalInterface
public interface Algorithm<I extends AlgorithmInput, O extends AlgorithmOutput> {

    O run(I input, AlgorithmContext context) throws InterruptedException;
}
