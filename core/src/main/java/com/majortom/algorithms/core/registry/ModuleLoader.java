package com.majortom.algorithms.core.registry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;

public final class ModuleLoader {

    public static final String RESOURCE_NAME = "META-INF/algorithms.factories";

    private ModuleLoader() {
    }

    public static ModuleRegistry load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ModuleLoader.class.getClassLoader();
        }
        return load(classLoader);
    }

    public static ModuleRegistry load(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        Map<String, Class<?>> implementations = new LinkedHashMap<>();
        for (URL resource : resources(classLoader)) {
            loadResource(classLoader, resource, implementations);
        }
        mergeDiscovered(classLoader, implementations);
        return new ModuleRegistry(implementations);
    }

    private static List<URL> resources(ClassLoader classLoader) {
        try {
            Enumeration<URL> enumeration = classLoader.getResources(RESOURCE_NAME);
            List<URL> resources = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                resources.add(enumeration.nextElement());
            }
            resources.sort(Comparator.comparing(URL::toString));
            return resources;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to enumerate " + RESOURCE_NAME, exception);
        }
    }

    private static void loadResource(ClassLoader classLoader, URL resource, Map<String, Class<?>> implementations) {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read module resource: " + resource, exception);
        }
        for (String key : properties.stringPropertyNames().stream().sorted().toList()) {
            String className = properties.getProperty(key).trim();
            if (key.isBlank() || className.isBlank()) {
                throw new IllegalStateException("Blank module mapping in " + resource);
            }
            Class<?> implementation = loadClass(classLoader, className, key, resource);
            Class<?> previous = implementations.putIfAbsent(key, implementation);
            if (previous != null && !previous.equals(implementation)) {
                throw new IllegalStateException("Duplicate module key '" + key + "': "
                        + previous.getName() + " vs " + implementation.getName());
            }
        }
    }

    private static void mergeDiscovered(ClassLoader classLoader, Map<String, Class<?>> implementations) {
        List<ModuleDiscovery> discoveries = ServiceLoader.load(ModuleDiscovery.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(discovery -> discovery.getClass().getName()))
                .toList();
        for (ModuleDiscovery discovery : discoveries) {
            Map<String, Class<?>> discovered = discovery.discover(classLoader);
            for (Map.Entry<String, Class<?>> entry : discovered.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).toList()) {
                String key = entry.getKey();
                Class<?> implementation = entry.getValue();
                if (implementations.containsValue(implementation)) {
                    continue;
                }
                Class<?> previous = implementations.putIfAbsent(key, implementation);
                if (previous != null && !previous.equals(implementation)) {
                    continue;
                }
            }
        }
    }

    private static Class<?> loadClass(ClassLoader classLoader, String className, String key, URL resource) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Implementation class not found for '" + key + "' in "
                    + resource + ": " + className, exception);
        }
    }
}
