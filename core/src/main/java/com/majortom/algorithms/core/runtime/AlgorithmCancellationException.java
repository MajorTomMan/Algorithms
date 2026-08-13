package com.majortom.algorithms.core.runtime;

/** Internal control-flow exception used to distinguish cancellation from failure. */
final class AlgorithmCancellationException extends RuntimeException {

    public AlgorithmCancellationException(String message) {
        super(message);
    }
}
