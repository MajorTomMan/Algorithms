package com.majortom.algorithms.visualization.runtime.value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Small explicit client-side adapter registry; it intentionally has no annotation/discovery layer. */
public final class ValueAdapters {
    private static final Map<Class<?>, ValueAdapter<?>> BY_TYPE = adapters();

    private ValueAdapters() {
    }

    public static List<Class<?>> supportedTypes() {
        return List.copyOf(BY_TYPE.keySet());
    }

    public static List<String> supportedTypeNames() {
        return BY_TYPE.keySet().stream().map(Class::getSimpleName).toList();
    }

    public static Class<?> requireType(String name) {
        Objects.requireNonNull(name, "name");
        return BY_TYPE.keySet().stream()
                .filter(type -> type.getSimpleName().equals(name) || type.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported Workbench value type: " + name));
    }

    @SuppressWarnings("unchecked")
    public static <T> ValueAdapter<T> require(Class<T> type) {
        Objects.requireNonNull(type, "type");
        ValueAdapter<?> adapter = BY_TYPE.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported Workbench value type: " + type.getName());
        }
        return (ValueAdapter<T>) adapter;
    }

    @SuppressWarnings("unchecked")
    public static ValueAdapter<Object> requireObjectAdapter(Class<?> type) {
        return (ValueAdapter<Object>) require((Class<Object>) type);
    }

    private static Map<Class<?>, ValueAdapter<?>> adapters() {
        LinkedHashMap<Class<?>, ValueAdapter<?>> adapters = new LinkedHashMap<>();
        register(adapters, new IntegerAdapter());
        register(adapters, new StringAdapter());
        return Map.copyOf(adapters);
    }

    private static <T> void register(Map<Class<?>, ValueAdapter<?>> adapters, ValueAdapter<T> adapter) {
        adapters.put(adapter.type(), adapter);
    }

    private static final class IntegerAdapter implements ValueAdapter<Integer> {
        @Override
        public Class<Integer> type() {
            return Integer.class;
        }

        @Override
        public Integer parse(String text) {
            return Integer.valueOf(Objects.requireNonNull(text, "text").trim());
        }

        @Override
        public String format(Integer value) {
            return String.valueOf(value);
        }
    }

    private static final class StringAdapter implements ValueAdapter<String> {
        @Override
        public Class<String> type() {
            return String.class;
        }

        @Override
        public String parse(String text) {
            return Objects.requireNonNull(text, "text").trim();
        }

        @Override
        public String format(String value) {
            return Objects.requireNonNull(value, "value");
        }
    }
}
