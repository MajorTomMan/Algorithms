package com.majortom.algorithms.practice.runtime.model;

import com.majortom.algorithms.core.problem.ProblemDifficulty;
import com.majortom.algorithms.core.problem.ProblemSource;

import java.lang.reflect.Method;
import java.util.Objects;

public record ProblemDescriptor(
        ProblemSource source,
        String id,
        String name,
        String number,
        ProblemDifficulty difficulty,
        java.util.List<String> tags,
        Class<?> implementation,
        Method entryPoint) {

    public ProblemDescriptor {
        source = Objects.requireNonNull(source, "source");
        id = requireText(id, "id");
        name = requireText(name, "name");
        number = number == null ? "" : number;
        difficulty = Objects.requireNonNull(difficulty, "difficulty");
        tags = tags == null ? java.util.List.of() : java.util.List.copyOf(tags);
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
