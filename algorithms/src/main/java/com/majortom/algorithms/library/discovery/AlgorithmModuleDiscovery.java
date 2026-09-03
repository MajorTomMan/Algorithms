package com.majortom.algorithms.library.discovery;

import com.majortom.algorithms.core.registry.ModuleDiscovery;
import com.majortom.algorithms.library.graph.GraphTraversal;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeGenerator;
import com.majortom.algorithms.library.sort.Sort;
import com.majortom.algorithms.library.string.StringSearch;
import com.majortom.algorithms.library.tree.TreeAlgorithm;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Discovers concrete project algorithms from the algorithms module at application startup. */
public final class AlgorithmModuleDiscovery implements ModuleDiscovery {

    private static final String PACKAGE_NAME = "com.majortom.algorithms.library";
    private static final String PACKAGE_PATH = PACKAGE_NAME.replace('.', '/');

    @Override
    public Map<String, Class<?>> discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        Map<String, Class<?>> registrations = new LinkedHashMap<>();
        for (Class<?> implementation : algorithmClasses(classLoader)) {
            Registration registration = registration(implementation);
            if (registration == null) {
                continue;
            }
            Class<?> previous = registrations.putIfAbsent(registration.key(), implementation);
            if (previous != null && !previous.equals(implementation)) {
                throw new IllegalStateException("Auto-discovered algorithm key collision '"
                        + registration.key() + "': " + previous.getName() + " vs " + implementation.getName());
            }
        }
        return Map.copyOf(registrations);
    }

    private List<Class<?>> algorithmClasses(ClassLoader classLoader) {
        List<String> classNames = classNames(classLoader);
        List<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> type = Class.forName(className, false, classLoader);
                if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
                    classes.add(type);
                }
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Unable to load auto-discovered algorithm class: " + className, exception);
            }
        }
        classes.sort(java.util.Comparator.comparing(Class::getName));
        return classes;
    }

    private List<String> classNames(ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(PACKAGE_PATH);
            List<String> names = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    collectDirectory(resource, names);
                } else if ("jar".equals(resource.getProtocol())) {
                    collectJar(resource, names);
                }
            }
            return names.stream().distinct().sorted().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan algorithm package " + PACKAGE_NAME, exception);
        }
    }

    private void collectDirectory(URL resource, List<String> names) {
        try {
            Path packageRoot = Path.of(resource.toURI());
            try (var paths = Files.walk(packageRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".class"))
                        .filter(path -> !path.getFileName().toString().contains("$"))
                        .forEach(path -> names.add(className(packageRoot, path)));
            }
        } catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException("Unable to scan algorithm class directory: " + resource, exception);
        }
    }

    private String className(Path packageRoot, Path classFile) {
        String relative = packageRoot.relativize(classFile).toString().replace(java.io.File.separatorChar, '.');
        return PACKAGE_NAME + "." + relative.substring(0, relative.length() - ".class".length());
    }

    private void collectJar(URL resource, List<String> names) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jar = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.startsWith(PACKAGE_PATH + "/") || !name.endsWith(".class") || name.contains("$")) {
                        continue;
                    }
                    names.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to scan algorithm jar: " + resource, exception);
        }
    }

    private Registration registration(Class<?> implementation) {
        if (Sort.class.isAssignableFrom(implementation)) {
            return registration("array", implementation, Sort.class, 0);
        }
        if (GraphTraversal.class.isAssignableFrom(implementation)) {
            return registration("graph", implementation, GraphTraversal.class, 0);
        }
        if (GraphMazeGenerator.class.isAssignableFrom(implementation)) {
            return registration("graph", implementation, GraphMazeGenerator.class, 0);
        }
        if (TreeAlgorithm.class.isAssignableFrom(implementation)) {
            return registration("tree", implementation, TreeAlgorithm.class, 0);
        }
        if (StringSearch.class.isAssignableFrom(implementation)) {
            return registration("string", "String", implementation);
        }
        if (ArrayMazeGenerator.class.isAssignableFrom(implementation)
                || ArrayMazePathfinder.class.isAssignableFrom(implementation)) {
            return registration("maze", "Boolean", implementation);
        }
        return null;
    }

    private Registration registration(String family, Class<?> implementation, Class<?> contract, int typeIndex) {
        Class<?> valueType = genericTypeArgument(implementation, contract, typeIndex);
        return registration(family, valueType.getSimpleName(), implementation);
    }

    private Registration registration(String family, String valueType, Class<?> implementation) {
        String id = derivedId(implementation.getSimpleName(), valueType);
        return new Registration("algorithm." + family + "." + valueType + "." + id);
    }

    private String derivedId(String simpleName, String valueType) {
        String kebab = simpleName
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
        String prefix = valueType.toLowerCase(Locale.ROOT) + "-";
        if (kebab.startsWith(prefix)) {
            kebab = kebab.substring(prefix.length());
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

    private Type findTypeArgument(Type current, Class<?> contract, int typeIndex,
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

    private Type findInHierarchy(Class<?> type, Class<?> contract, int typeIndex,
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

    private record Registration(String key) {
    }
}
