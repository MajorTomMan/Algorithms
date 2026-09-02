package com.majortom.algorithms.core.registry;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ModuleLoader {

    public static final String RESOURCE_NAME = "META-INF/algorithms.factories";

    private ModuleLoader() {
    }

    public static ModuleRegistry load() {
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        if (c == null) c = ModuleLoader.class.getClassLoader();
        return load(c);
    }

    public static ModuleRegistry load(ClassLoader c) {
        Objects.requireNonNull(c);
        Map<String, Class<?>> m = new LinkedHashMap<>();
        for (URL u : resources(c)) loadResource(c, u, m);
        return new ModuleRegistry(m);
    }

    private static List<URL> resources(ClassLoader c) {
        try {
            Enumeration<URL> e = c.getResources(RESOURCE_NAME);
            List<URL> r = new ArrayList<>();
            while (e.hasMoreElements()) r.add(e.nextElement());
            r.sort(Comparator.comparing(URL::toString));
            return r;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to enumerate " + RESOURCE_NAME, e);
        }
    }

    private static void loadResource(ClassLoader c, URL u, Map<String, Class<?>> m) {
        Properties p = new Properties();
        try (InputStreamReader r = new InputStreamReader(u.openStream(), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read module resource: " + u, e);
        }

        for (String k : p.stringPropertyNames().stream().sorted().toList()) {
            String n = p.getProperty(k).trim();
            if (k.isBlank() || n.isBlank()) throw new IllegalStateException("Blank module mapping in " + u);
            Class<?> impl = loadClass(c, n, k, u);
            Class<?> prev = m.putIfAbsent(k, impl);
            if (prev != null && !prev.equals(impl)) {
                throw new IllegalStateException("Duplicate module key '" + k + "': " + prev.getName() + " vs " + impl.getName());
            }
        }
    }

    private static Class<?> loadClass(ClassLoader c, String n, String k, URL u) {
        try {
            return Class.forName(n, false, c);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Implementation class not found for '" + k + "' in " + u + ": " + n, e);
        }
    }
}
