package com.majortom.algorithms.core.registry;

import java.util.Objects;

public record StructureDescriptor(
        String id,
        Class<?> contract,
        Class<?> implementation) {

    public StructureDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(implementation, "implementation");
    }
}
