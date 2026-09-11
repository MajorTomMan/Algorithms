package com.majortom.algorithms.core.registry;

import java.util.Objects;

public record AlgorithmDescriptor(
        String id,
        Class<?> valueType,
        Class<?> structureContract,
        Class<?> implementation) {

    public AlgorithmDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(structureContract, "structureContract");
        Objects.requireNonNull(implementation, "implementation");
    }
}
