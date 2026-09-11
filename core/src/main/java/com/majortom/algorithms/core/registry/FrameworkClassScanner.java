package com.majortom.algorithms.core.registry;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FrameworkClassScanner {

    public List<Class<?>> scan(String rootPackage) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = FrameworkClassScanner.class.getClassLoader();
        }
        return scan(rootPackage, classLoader);
    }

    public List<Class<?>> scan(String rootPackage, ClassLoader classLoader) {
        Objects.requireNonNull(rootPackage, "rootPackage");
        Objects.requireNonNull(classLoader, "classLoader");
        if (rootPackage.isBlank()) {
            throw new IllegalArgumentException("rootPackage must not be blank");
        }
        String packagePath = rootPackage.replace('.', '/');
        List<String> classNames = classNames(rootPackage, packagePath, classLoader);
        List<Class<?>> classes = new ArrayList<>(classNames.size());
        for (String className : classNames) {
            try {
                classes.add(Class.forName(className, false, classLoader));
            } catch (ClassNotFoundException exception) {
                throw new RegistrationException("Unable to load framework class: " + className, exception);
            }
        }
        return List.copyOf(classes);
    }

    private List<String> classNames(String rootPackage, String packagePath, ClassLoader classLoader) {
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            List<String> names = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if ("file".equals(resource.getProtocol())) {
                    collectDirectory(rootPackage, resource, names);
                } else if ("jar".equals(resource.getProtocol())) {
                    collectJar(packagePath, resource, names);
                }
            }
            return names.stream().distinct().sorted().toList();
        } catch (IOException exception) {
            throw new RegistrationException("Unable to scan framework package " + rootPackage, exception);
        }
    }

    private void collectDirectory(String rootPackage, URL resource, List<String> names) {
        try {
            Path packageRoot = Path.of(resource.toURI());
            try (var paths = Files.walk(packageRoot)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> isCandidateClass(path.getFileName().toString()))
                        .forEach(path -> names.add(className(rootPackage, packageRoot, path)));
            }
        } catch (IOException | URISyntaxException exception) {
            throw new RegistrationException("Unable to scan framework class directory: " + resource, exception);
        }
    }

    private String className(String rootPackage, Path packageRoot, Path classFile) {
        String relative = packageRoot.relativize(classFile).toString().replace(java.io.File.separatorChar, '.');
        return rootPackage + "." + relative.substring(0, relative.length() - ".class".length());
    }

    private void collectJar(String packagePath, URL resource, List<String> names) {
        try {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            try (JarFile jar = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.startsWith(packagePath + "/") || !isCandidateClass(name)) {
                        continue;
                    }
                    names.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                }
            }
        } catch (IOException exception) {
            throw new RegistrationException("Unable to scan framework jar: " + resource, exception);
        }
    }

    private boolean isCandidateClass(String name) {
        return name.endsWith(".class")
                && !name.contains("$")
                && !name.endsWith("package-info.class")
                && !name.endsWith("module-info.class");
    }
}
