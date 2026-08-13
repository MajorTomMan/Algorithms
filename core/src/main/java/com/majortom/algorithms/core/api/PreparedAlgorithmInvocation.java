package com.majortom.algorithms.core.api;

/** A type-safe invocation created only after erased input validation succeeds. */
@FunctionalInterface
public interface PreparedAlgorithmInvocation {

    AlgorithmOutput invoke(AlgorithmContext context) throws InterruptedException;
}
