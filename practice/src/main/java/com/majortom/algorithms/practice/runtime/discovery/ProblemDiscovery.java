package com.majortom.algorithms.practice.runtime.discovery;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.metadata.ComponentNames;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ProblemDiscovery {
    private final List<String> rootPackages;
    private final FrameworkClassScanner scanner;

    public ProblemDiscovery(String... rootPackages) {
        this(Arrays.asList(rootPackages));
    }

    public ProblemDiscovery(List<String> rootPackages) {
        this(rootPackages, new FrameworkClassScanner());
    }

    ProblemDiscovery(List<String> rootPackages, FrameworkClassScanner scanner) {
        Objects.requireNonNull(rootPackages, "rootPackages");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        LinkedHashSet<String> roots = new LinkedHashSet<>();
        for (String rootPackage : rootPackages) {
            Objects.requireNonNull(rootPackage, "rootPackage");
            if (rootPackage.isBlank()) {
                throw new IllegalArgumentException("rootPackage must not be blank");
            }
            roots.add(rootPackage);
        }
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("At least one problem root package is required");
        }
        this.rootPackages = List.copyOf(roots);
    }

    public List<String> rootPackages() {
        return rootPackages;
    }

    public List<ProblemDescriptor> discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        LinkedHashSet<Class<?>> candidates = new LinkedHashSet<>();
        for (String rootPackage : rootPackages) {
            candidates.addAll(scanner.scan(rootPackage, classLoader));
        }

        List<ProblemDescriptor> descriptors = new ArrayList<>();
        for (Class<?> candidate : candidates) {
            Problem problem = candidate.getAnnotation(Problem.class);
            if (problem == null) {
                continue;
            }
            validateConcrete(candidate);
            Method entry = findEntry(candidate);
            descriptors.add(new ProblemDescriptor(problem.source(), problem.id(), ComponentNames.resolve(problem.name(), candidate), problem.number(), problem.difficulty(), Arrays.asList(problem.tags()), candidate, entry));
        }
        descriptors.sort(Comparator.comparing(ProblemDescriptor::stableId));
        validateUniqueIds(descriptors);
        return List.copyOf(descriptors);
    }

    private static Method findEntry(Class<?> type) {
        List<Method> entries = java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(ProblemEntry.class))
                .toList();
        if (entries.size() != 1) {
            throw new RegistrationException("Practice problem " + type.getName()
                    + " must declare exactly one @ProblemEntry method, found " + entries.size());
        }
        Method entry = entries.getFirst();
        if (entry.isSynthetic() || entry.isBridge()) {
            throw new RegistrationException("@ProblemEntry must be a concrete source method: " + entry);
        }
        entry.trySetAccessible();
        return entry;
    }

    private static void validateConcrete(Class<?> type) {
        int modifiers = type.getModifiers();
        if (type.isInterface() || Modifier.isAbstract(modifiers)) {
            throw new RegistrationException("@Problem type must be concrete: " + type.getName());
        }
    }

    private static void validateUniqueIds(List<ProblemDescriptor> descriptors) {
        Set<String> ids = new LinkedHashSet<>();
        for (ProblemDescriptor descriptor : descriptors) {
            if (!ids.add(descriptor.stableId())) {
                throw new RegistrationException("Duplicate Practice problem id: " + descriptor.stableId());
            }
        }
    }
}
