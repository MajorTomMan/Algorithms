package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;

import java.util.List;

/** Structure-driven algorithm lookup for the workbench. */
public final class AlgorithmCatalog {

    private static final ComponentRegistry REGISTRY = ComponentDiscovery.discover();

    private AlgorithmCatalog() {
    }

    public static String name(String algorithmId) {
        List<AlgorithmDescriptor> matches = REGISTRY.algorithms().stream()
                .filter(descriptor -> descriptor.id().equals(algorithmId))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No Algorithm registered for id: " + algorithmId);
        }
        List<String> names = matches.stream().map(AlgorithmDescriptor::name).distinct().toList();
        if (names.size() != 1) {
            throw new IllegalArgumentException("Algorithm id has multiple display names across registrations: "
                    + algorithmId + " -> " + names);
        }
        return names.getFirst();
    }

    public static AlgorithmDescriptor descriptor(String moduleId, Class<?> valueType, String algorithmId) {
        return REGISTRY.requireAlgorithm(StructureModule.fromId(moduleId), valueType, algorithmId);
    }

    public static AlgorithmDescriptor descriptor(String moduleId, String algorithmId) {
        StructureModule module = StructureModule.fromId(moduleId);
        List<AlgorithmDescriptor> matches = REGISTRY.algorithms().stream()
                .filter(descriptor -> descriptor.module() == module && descriptor.id().equals(algorithmId))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one Algorithm for module=" + module.id()
                    + ", id=" + algorithmId + ", found=" + matches.size());
        }
        return matches.getFirst();
    }

    public static AlgorithmDescriptor compatibleDescriptor(
            Class<?> activeStructure, Class<?> valueType, String algorithmId) {
        List<AlgorithmDescriptor> matches = REGISTRY.compatibleAlgorithms(activeStructure, valueType).stream()
                .filter(descriptor -> descriptor.id().equals(algorithmId))
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one compatible Algorithm for structure="
                    + activeStructure.getName() + ", type=" + valueType.getName() + ", id=" + algorithmId
                    + ", found=" + matches.size());
        }
        return matches.getFirst();
    }

    public static String name(String moduleId, Class<?> valueType, String algorithmId) {
        return descriptor(moduleId, valueType, algorithmId).name();
    }

    public static List<String> forWorkbenchModule(String moduleId) {
        return descriptorsForWorkbenchModule(moduleId).stream()
                .map(AlgorithmDescriptor::id)
                .distinct()
                .toList();
    }

    static List<AlgorithmDescriptor> descriptorsForWorkbenchModule(String moduleId) {
        StructureModule module = StructureModule.fromId(moduleId);
        return REGISTRY.algorithms().stream()
                .filter(descriptor -> descriptor.module() == module)
                .toList();
    }

    public static List<String> forWorkbenchModule(String moduleId, Class<?> valueType) {
        return REGISTRY.algorithms(StructureModule.fromId(moduleId), valueType).stream()
                .map(AlgorithmDescriptor::id)
                .distinct()
                .toList();
    }

    public static List<String> compatibleAlgorithms(Class<?> activeStructure, Class<?> valueType) {
        return REGISTRY.compatibleAlgorithms(activeStructure, valueType).stream()
                .map(AlgorithmDescriptor::id)
                .distinct()
                .toList();
    }
}
