package com.majortom.algorithms.practice.runtime.discovery;

import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.practice.runtime.annotation.Problem;
import com.majortom.algorithms.practice.runtime.annotation.ProblemEntry;
import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ProblemDiscovery {
    public static final String ROOT_PACKAGE = "com.majortom.algorithms.practice";
    private final FrameworkClassScanner scanner = new FrameworkClassScanner();

    public List<ProblemDescriptor> discover(ClassLoader classLoader) {
        List<ProblemDescriptor> descriptors = new ArrayList<>();
        for (Class<?> candidate : scanner.scan(ROOT_PACKAGE, classLoader)) {
            Problem problem = candidate.getAnnotation(Problem.class);
            if (problem == null) continue;
            validateConcrete(candidate);
            Method entry = findEntry(candidate);
            descriptors.add(new ProblemDescriptor(problem.source(), problem.id(), problem.title(), candidate, entry));
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
