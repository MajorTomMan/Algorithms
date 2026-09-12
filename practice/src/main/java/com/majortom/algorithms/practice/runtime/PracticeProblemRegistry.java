package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.practice.runtime.discovery.ProblemDiscovery;
import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import com.majortom.algorithms.practice.runtime.model.ProblemSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PracticeProblemRegistry {
    private final Map<String, ProblemDescriptor> descriptors;

    public PracticeProblemRegistry(List<ProblemDescriptor> descriptors) {
        Objects.requireNonNull(descriptors, "descriptors");
        LinkedHashMap<String, ProblemDescriptor> indexed = new LinkedHashMap<>();
        for (ProblemDescriptor descriptor : descriptors) {
            ProblemDescriptor previous = indexed.putIfAbsent(descriptor.stableId(), descriptor);
            if (previous != null) throw new IllegalArgumentException("Duplicate problem id: " + descriptor.stableId());
        }
        this.descriptors = Map.copyOf(indexed);
    }

    public static PracticeProblemRegistry discover() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = PracticeProblemRegistry.class.getClassLoader();
        return discover(loader);
    }

    public static PracticeProblemRegistry discover(ClassLoader loader) {
        return new PracticeProblemRegistry(new ProblemDiscovery().discover(loader));
    }

    public List<ProblemDescriptor> problems() {
        return descriptors.values().stream().sorted(java.util.Comparator.comparing(ProblemDescriptor::stableId)).toList();
    }

    public ProblemDescriptor require(ProblemSource source, String id) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(id, "id");
        String key = source.name().toLowerCase(java.util.Locale.ROOT) + ":" + id;
        ProblemDescriptor descriptor = descriptors.get(key);
        if (descriptor == null) throw new IllegalArgumentException("No Practice problem registered for " + key);
        return descriptor;
    }
}
