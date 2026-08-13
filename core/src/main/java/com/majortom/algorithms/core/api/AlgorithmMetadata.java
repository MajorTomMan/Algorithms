package com.majortom.algorithms.core.api;

import java.util.Objects;

/** Stable, presentation-neutral metadata for an algorithm. */
public record AlgorithmMetadata(String id, String moduleId, String version) {

    public AlgorithmMetadata {
        id = requireText(id, "id");
        moduleId = requireText(moduleId, "moduleId");
        version = requireText(version, "version");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
