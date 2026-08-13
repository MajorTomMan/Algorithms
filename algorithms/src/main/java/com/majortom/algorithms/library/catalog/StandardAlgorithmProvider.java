package com.majortom.algorithms.library.catalog;

import com.majortom.algorithms.core.api.Algorithm;
import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmMetadata;
import com.majortom.algorithms.core.api.AlgorithmOutput;
import com.majortom.algorithms.core.api.AlgorithmProvider;

import java.util.Objects;
import java.util.function.Supplier;

/** Immutable provider implementation used by the explicit production catalog. */
public final class StandardAlgorithmProvider<I extends AlgorithmInput, O extends AlgorithmOutput>
        implements AlgorithmProvider<I, O> {

    private final AlgorithmMetadata metadata;
    private final Class<I> inputType;
    private final Class<O> outputType;
    private final Supplier<? extends Algorithm<I, O>> factory;

    public StandardAlgorithmProvider(
            AlgorithmMetadata metadata,
            Class<I> inputType,
            Class<O> outputType,
            Supplier<? extends Algorithm<I, O>> factory) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.inputType = Objects.requireNonNull(inputType, "inputType");
        this.outputType = Objects.requireNonNull(outputType, "outputType");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public AlgorithmMetadata metadata() {
        return metadata;
    }

    @Override
    public Class<I> inputType() {
        return inputType;
    }

    @Override
    public Class<O> outputType() {
        return outputType;
    }

    @Override
    public Algorithm<I, O> createAlgorithm() {
        return Objects.requireNonNull(factory.get(), "factory result");
    }
}
