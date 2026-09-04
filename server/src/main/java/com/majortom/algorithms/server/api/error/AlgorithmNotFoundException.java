package com.majortom.algorithms.server.api.error;

public final class AlgorithmNotFoundException extends RuntimeException {
    public AlgorithmNotFoundException(String algorithmId) {
        super("Algorithm is not available: " + algorithmId);
    }
}
