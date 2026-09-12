package com.majortom.algorithms.core.metadata;

import java.util.Objects;

/** Small formatting helper for annotation metadata name fallbacks. */
public final class ComponentNames {
    private ComponentNames() {
    }

    public static String resolve(String explicitName, Class<?> type) {
        Objects.requireNonNull(type, "type");
        if (explicitName != null && !explicitName.isBlank()) {
            return explicitName.trim();
        }
        return fromClassName(type.getSimpleName());
    }

    public static String fromClassName(String className) {
        Objects.requireNonNull(className, "className");
        String value = className.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("className must not be blank");
        }
        value = value.replace('_', ' ');
        value = value.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        value = value.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");
        return value.replaceAll("\\s+", " ").trim();
    }
}
