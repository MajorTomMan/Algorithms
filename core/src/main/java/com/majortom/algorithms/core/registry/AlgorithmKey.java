package com.majortom.algorithms.core.registry;

import java.util.Objects;

/** Stable runtime identity of an Algorithm registration. */
public record AlgorithmKey(
        String moduleId,
        Class<?> valueType,
        String algorithmId) {

    public AlgorithmKey {
        moduleId = requireText(moduleId, "moduleId");
        valueType = Objects.requireNonNull(valueType, "valueType");
        algorithmId = requireText(algorithmId, "algorithmId");
    }

    public static AlgorithmKey of(AlgorithmDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        return new AlgorithmKey(descriptor.moduleId(), descriptor.valueType(), descriptor.id());
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
