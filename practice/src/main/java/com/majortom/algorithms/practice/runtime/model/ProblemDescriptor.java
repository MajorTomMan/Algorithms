package com.majortom.algorithms.practice.runtime.model;

import java.lang.reflect.Method;
import java.util.Objects;

public record ProblemDescriptor(
        ProblemSource source,
        String id,
        String title,
        Class<?> implementation,
        Method entryPoint) {

    public ProblemDescriptor {
        source = Objects.requireNonNull(source, "source");
        id = requireText(id, "id");
        title = requireText(title, "title");
        implementation = Objects.requireNonNull(implementation, "implementation");
        entryPoint = Objects.requireNonNull(entryPoint, "entryPoint");
        if (!entryPoint.getDeclaringClass().equals(implementation)) {
            throw new IllegalArgumentException("entryPoint must be declared by implementation");
        }
    }

    public String stableId() {
        return source.name().toLowerCase(java.util.Locale.ROOT) + ":" + id;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
