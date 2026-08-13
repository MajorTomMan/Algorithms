package com.majortom.algorithms.core.api;

/** The only execution environment visible to an algorithm implementation. */
public interface AlgorithmContext {

    String runId();

    String algorithmId();

    void emit(AlgorithmEvent event);

    void checkpoint() throws InterruptedException;
}
