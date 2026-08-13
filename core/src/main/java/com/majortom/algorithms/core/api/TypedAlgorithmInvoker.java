package com.majortom.algorithms.core.api;

import java.util.Objects;

/** Contains the single controlled type-erasure boundary for a typed provider. */
public final class TypedAlgorithmInvoker<I extends AlgorithmInput, O extends AlgorithmOutput>
        implements AlgorithmInvoker {

    private final AlgorithmProvider<I, O> provider;
    private final AlgorithmMetadata metadata;
    private final Class<I> inputType;
    private final Class<O> outputType;
    private final InputValidator<I> inputValidator;

    public TypedAlgorithmInvoker(AlgorithmProvider<I, O> provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.metadata = Objects.requireNonNull(provider.metadata(), "provider.metadata");
        this.inputType = Objects.requireNonNull(provider.inputType(), "provider.inputType");
        this.outputType = Objects.requireNonNull(provider.outputType(), "provider.outputType");
        this.inputValidator = Objects.requireNonNull(provider.inputValidator(), "provider.inputValidator");
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
    public PreparedAlgorithmInvocation prepare(AlgorithmInput input) {
        I typedInput = castInput(input);
        try {
            inputValidator.validate(typedInput);
        } catch (AlgorithmInvocationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            String message = "Invalid input for algorithm '" + metadata().id() + "': " + exception.getMessage();
            throw new AlgorithmInvocationException("algorithm.input.invalid", message);
        }
        return context -> invokePrepared(typedInput, context);
    }

    private O invokePrepared(I typedInput, AlgorithmContext context) throws InterruptedException {
        Objects.requireNonNull(context, "context");
        Algorithm<I, O> algorithm = Objects.requireNonNull(provider.createAlgorithm(), "provider.createAlgorithm");
        O output = algorithm.run(typedInput, context);
        if (output == null) {
            String message = "Algorithm '" + metadata().id() + "' returned null instead of "
                    + outputType().getName();
            throw new AlgorithmInvocationException("algorithm.output.null", message);
        }
        try {
            return outputType().cast(output);
        } catch (ClassCastException exception) {
            String message = "Algorithm '" + metadata().id() + "' returned " + output.getClass().getName()
                    + ", expected " + outputType().getName();
            throw new AlgorithmInvocationException("algorithm.output.type-mismatch", message);
        }
    }

    private I castInput(AlgorithmInput input) {
        if (input == null) {
            String message = "Algorithm '" + metadata().id() + "' requires non-null input of type "
                    + inputType().getName();
            throw new AlgorithmInvocationException("algorithm.input.null", message);
        }
        try {
            return inputType().cast(input);
        } catch (ClassCastException exception) {
            String message = "Algorithm '" + metadata().id() + "' received " + input.getClass().getName()
                    + ", expected " + inputType().getName();
            throw new AlgorithmInvocationException("algorithm.input.type-mismatch", message);
        }
    }
}
