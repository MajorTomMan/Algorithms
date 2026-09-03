package com.majortom.algorithms.core.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ModuleRegistry {

    private static final String STRUCTURE_PREFIX = "structure.";
    private static final String ALGORITHM_PREFIX = "algorithm.";
    private static final List<String> VALUE_TYPES = List.of(
            "Integer", "Long", "Double", "Float", "Boolean",
            "Character", "Byte", "Short", "String");

    private final Map<String, Class<?>> implementations;

    ModuleRegistry(Map<String, Class<?>> implementations) {
        LinkedHashMap<String, Class<?>> copy = new LinkedHashMap<>(implementations);
        validateAlgorithmValueTypes(copy.keySet());
        this.implementations = Map.copyOf(copy);
    }


    public static List<String> valueTypes() {
        return VALUE_TYPES;
    }

    public boolean contains(String key) {
        return implementations.containsKey(requireKey(key));
    }

    public Optional<Class<?>> find(String key) {
        return Optional.ofNullable(implementations.get(requireKey(key)));
    }

    public Class<?> require(String key) {
        String normalizedKey = requireKey(key);
        Class<?> implementation = implementations.get(normalizedKey);
        if (implementation == null) {
            throw new IllegalArgumentException("No implementation registered for: " + normalizedKey);
        }
        return implementation;
    }

    public <T> Class<? extends T> require(String key, Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        Class<?> implementation = require(key);
        if (!contract.isAssignableFrom(implementation)) {
            throw new IllegalStateException("Registered implementation " + implementation.getName()
                    + " does not implement " + contract.getName() + " for key " + key);
        }
        return implementation.asSubclass(contract);
    }

    public <T> T create(String key, Class<T> contract) {
        Class<? extends T> implementation = require(key, contract);
        try {
            return implementation.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Registered implementation requires an accessible no-arg constructor: "
                    + implementation.getName(), exception);
        }
    }

    public List<String> keys(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return implementations.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .toList();
    }

    public List<String> structureFamilies() {
        Set<String> families = new LinkedHashSet<>();
        for (String key : keys(STRUCTURE_PREFIX)) {
            String family = segmentAfterPrefix(key, STRUCTURE_PREFIX);
            if (family != null) {
                families.add(family);
            }
        }
        return List.copyOf(families);
    }

    public boolean hasStructureFamily(String family) {
        return !keys(STRUCTURE_PREFIX + requireSegment(family, "family") + ".").isEmpty();
    }

    public boolean hasAlgorithmFamily(String family) {
        return !keys(ALGORITHM_PREFIX + requireSegment(family, "family") + ".").isEmpty();
    }

    public List<String> structureTypeSignatures(String family) {
        String prefix = STRUCTURE_PREFIX + requireSegment(family, "family") + ".";
        List<String> signatures = new ArrayList<>();
        for (String key : keys(prefix)) {
            signatures.add(key.substring(prefix.length()));
        }
        return List.copyOf(signatures);
    }

    public List<String> algorithmValueTypes(String family) {
        String prefix = ALGORITHM_PREFIX + requireSegment(family, "family") + ".";
        Set<String> valueTypes = new LinkedHashSet<>();
        for (String key : keys(prefix)) {
            String valueType = segmentAfterPrefix(key, prefix);
            if (valueType != null) {
                valueTypes.add(valueType);
            }
        }
        return List.copyOf(valueTypes);
    }

    public List<String> algorithmIds(String family, String valueType) {
        String prefix = ALGORITHM_PREFIX + requireSegment(family, "family") + "."
                + requireSegment(valueType, "valueType") + ".";
        List<String> ids = new ArrayList<>();
        for (String key : keys(prefix)) {
            ids.add(key.substring(prefix.length()));
        }
        return List.copyOf(ids);
    }

    public Map<String, Class<?>> entries() {
        return implementations;
    }


    private static void validateAlgorithmValueTypes(Set<String> keys) {
        for (String key : keys) {
            if (!key.startsWith(ALGORITHM_PREFIX)) {
                continue;
            }
            String suffix = key.substring(ALGORITHM_PREFIX.length());
            int familySeparator = suffix.indexOf('.');
            if (familySeparator < 1) {
                throw new IllegalArgumentException("Invalid algorithm registry key: " + key);
            }
            int typeSeparator = suffix.indexOf('.', familySeparator + 1);
            if (typeSeparator < 0) {
                throw new IllegalArgumentException("Invalid algorithm registry key: " + key);
            }
            String valueType = suffix.substring(familySeparator + 1, typeSeparator);
            if (!VALUE_TYPES.contains(valueType)) {
                throw new IllegalArgumentException("Unsupported Registry ValueType '" + valueType + "' in " + key);
            }
        }
    }

    private static String segmentAfterPrefix(String key, String prefix) {
        String suffix = key.substring(prefix.length());
        int separator = suffix.indexOf('.');
        if (separator <= 0) {
            return null;
        }
        return suffix.substring(0, separator);
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return key;
    }

    private static String requireSegment(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.indexOf('.') >= 0) {
            throw new IllegalArgumentException(name + " must be a non-blank registry segment");
        }
        return value;
    }
}
