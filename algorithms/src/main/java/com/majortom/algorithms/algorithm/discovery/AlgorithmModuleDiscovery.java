package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.algorithm.array.ArrayAlgorithm;
import com.majortom.algorithms.algorithm.graph.GraphFamilyAlgorithm;
import com.majortom.algorithms.algorithm.maze.ArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.ArrayMazePathfinder;
import com.majortom.algorithms.algorithm.maze.GraphMazeGenerator;
import com.majortom.algorithms.algorithm.string.StringAlgorithm;
import com.majortom.algorithms.algorithm.tree.TreeFamilyAlgorithm;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.ModuleDiscovery;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Transitional legacy-key bridge backed by annotation discovery for migrated algorithms. */
public final class AlgorithmModuleDiscovery implements ModuleDiscovery {
    private final AlgorithmDiscovery annotationDiscovery = new AlgorithmDiscovery();
    private final FrameworkClassScanner scanner = new FrameworkClassScanner();

    @Override
    public Map<String, Class<?>> discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        Map<String, Class<?>> registrations = new LinkedHashMap<>();
        for (AlgorithmDescriptor descriptor : annotationDiscovery.discover(classLoader)) {
            add(registrations, annotatedRegistration(descriptor));
        }
        for (Class<?> implementation : scanner.scan(AlgorithmDiscovery.ROOT_PACKAGE, classLoader)) {
            if (implementation.isAnnotationPresent(Algorithm.class)
                    || implementation.isInterface()
                    || Modifier.isAbstract(implementation.getModifiers())) {
                continue;
            }
            add(registrations, legacyRegistration(implementation));
        }
        return Map.copyOf(registrations);
    }

    private Registration annotatedRegistration(AlgorithmDescriptor descriptor) {
        String family = legacyFamily(descriptor.implementation());
        if (family == null) {
            return null;
        }
        return new Registration(
                "algorithm." + family + "." + descriptor.valueType().getSimpleName() + "." + descriptor.id(),
                descriptor.implementation());
    }

    private String legacyFamily(Class<?> implementation) {
        if (ArrayAlgorithm.class.isAssignableFrom(implementation)) {
            return "array";
        }
        if (GraphFamilyAlgorithm.class.isAssignableFrom(implementation)) {
            return "graph";
        }
        if (TreeFamilyAlgorithm.class.isAssignableFrom(implementation)) {
            return "tree";
        }
        if (StringAlgorithm.class.isAssignableFrom(implementation)) {
            return "string";
        }
        return null;
    }

    private Registration legacyRegistration(Class<?> implementation) {
        if (ArrayAlgorithm.class.isAssignableFrom(implementation)) {
            return legacyRegistration("array", implementation, ArrayAlgorithm.class, 0);
        }
        if (GraphFamilyAlgorithm.class.isAssignableFrom(implementation)) {
            return legacyRegistration("graph", implementation, GraphFamilyAlgorithm.class, 0);
        }
        if (GraphMazeGenerator.class.isAssignableFrom(implementation)) {
            return legacyRegistration("graph", implementation, GraphMazeGenerator.class, 0);
        }
        if (TreeFamilyAlgorithm.class.isAssignableFrom(implementation)) {
            return legacyRegistration("tree", implementation, TreeFamilyAlgorithm.class, 0);
        }
        if (StringAlgorithm.class.isAssignableFrom(implementation)) {
            return legacyRegistration("string", "String", implementation);
        }
        if (ArrayMazeGenerator.class.isAssignableFrom(implementation)
                || ArrayMazePathfinder.class.isAssignableFrom(implementation)) {
            return legacyRegistration("maze", "Boolean", implementation);
        }
        return null;
    }

    private Registration legacyRegistration(
            String family,
            Class<?> implementation,
            Class<?> contract,
            int typeIndex) {
        Class<?> valueType = genericTypeArgument(implementation, contract, typeIndex);
        return legacyRegistration(family, valueType.getSimpleName(), implementation);
    }

    private Registration legacyRegistration(String family, String valueType, Class<?> implementation) {
        String id = derivedId(implementation.getSimpleName(), valueType);
        return new Registration("algorithm." + family + "." + valueType + "." + id, implementation);
    }

    private String derivedId(String simpleName, String valueType) {
        String kebab = simpleName
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase(java.util.Locale.ROOT);
        String prefix = valueType.toLowerCase(java.util.Locale.ROOT) + "-";
        if (kebab.startsWith(prefix)) {
            return kebab.substring(prefix.length());
        }
        return kebab;
    }

    private Class<?> genericTypeArgument(Class<?> implementation, Class<?> contract, int typeIndex) {
        Type result = findTypeArgument(implementation, contract, typeIndex, new HashMap<>());
        if (result instanceof Class<?> type) {
            return type;
        }
        throw new IllegalStateException("Cannot resolve value type for " + implementation.getName()
                + " through " + contract.getName());
    }

    private Type findTypeArgument(
            Type current,
            Class<?> contract,
            int typeIndex,
            Map<TypeVariable<?>, Type> bindings) {
        if (current instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            Map<TypeVariable<?>, Type> nestedBindings = new HashMap<>(bindings);
            TypeVariable<?>[] variables = rawType.getTypeParameters();
            Type[] arguments = parameterizedType.getActualTypeArguments();
            for (int index = 0; index < variables.length; index++) {
                nestedBindings.put(variables[index], resolve(arguments[index], bindings));
            }
            if (rawType.equals(contract)) {
                return resolve(arguments[typeIndex], nestedBindings);
            }
            Type found = findInHierarchy(rawType, contract, typeIndex, nestedBindings);
            if (found != null) {
                return found;
            }
        } else if (current instanceof Class<?> rawType) {
            if (rawType.equals(contract)) {
                return null;
            }
            Type found = findInHierarchy(rawType, contract, typeIndex, bindings);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Type findInHierarchy(
            Class<?> type,
            Class<?> contract,
            int typeIndex,
            Map<TypeVariable<?>, Type> bindings) {
        for (Type interfaceType : type.getGenericInterfaces()) {
            Type found = findTypeArgument(interfaceType, contract, typeIndex, bindings);
            if (found != null) {
                return found;
            }
        }
        Type superclass = type.getGenericSuperclass();
        if (superclass != null) {
            return findTypeArgument(superclass, contract, typeIndex, bindings);
        }
        return null;
    }

    private Type resolve(Type type, Map<TypeVariable<?>, Type> bindings) {
        Type resolved = type;
        while (resolved instanceof TypeVariable<?> variable && bindings.containsKey(variable)) {
            resolved = bindings.get(variable);
        }
        return resolved;
    }

    private void add(Map<String, Class<?>> registrations, Registration registration) {
        if (registration == null) {
            return;
        }
        Class<?> previous = registrations.putIfAbsent(registration.key(), registration.implementation());
        if (previous != null && !previous.equals(registration.implementation())) {
            throw new IllegalStateException("Algorithm key collision '" + registration.key()
                    + "': " + previous.getName() + " vs " + registration.implementation().getName());
        }
    }

    private record Registration(String key, Class<?> implementation) {
    }
}
