package com.majortom.algorithms.core.api;

/** Type-erased boundary used by catalogs and dynamic launchers. */
public interface AlgorithmInvoker {

    AlgorithmMetadata metadata();

    Class<? extends AlgorithmInput> inputType();

    Class<? extends AlgorithmOutput> outputType();

    PreparedAlgorithmInvocation prepare(AlgorithmInput input);

    default AlgorithmOutput invoke(AlgorithmInput input, AlgorithmContext context) throws InterruptedException {
        return prepare(input).invoke(context);
    }
}
