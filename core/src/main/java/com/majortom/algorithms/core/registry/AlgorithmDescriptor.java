package com.majortom.algorithms.core.registry;

import java.util.Objects;
import java.util.Optional;

public record AlgorithmDescriptor(
        String id,
        String name,
        String moduleId,
        Class<?> valueType,
        Class<?> structureContract,
        Class<?> implementation) {

    public AlgorithmDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(moduleId, "moduleId");
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(structureContract, "structureContract");
        Objects.requireNonNull(implementation, "implementation");
    }

    public AlgorithmKey key() {
        return AlgorithmKey.of(this);
    }

    public boolean hasStructureContract() {
        return structureContract != Void.class;
    }

    public Optional<Class<?>> optionalStructureContract() {
        if (!hasStructureContract()) {
            return Optional.empty();
        }
        return Optional.of(structureContract);
    }
}
