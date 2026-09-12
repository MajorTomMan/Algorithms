package com.majortom.algorithms.core.registry;

import java.util.Objects;

public record StructureDescriptor(
        String id,
        String name,
        Class<?> contract,
        Class<?> implementation) {

    public StructureDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(implementation, "implementation");
    }
}
