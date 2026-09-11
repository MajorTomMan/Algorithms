package com.majortom.algorithms.core.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ComponentRegistry {
    private final Map<String, StructureDescriptor> structuresById;
    private final Map<String, AlgorithmDescriptor> algorithmsById;

    public ComponentRegistry(
            List<StructureDescriptor> structures,
            List<AlgorithmDescriptor> algorithms) {
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(algorithms, "algorithms");
        RegistrationValidator.validateUniqueStructureIds(structures);
        RegistrationValidator.validateUniqueAlgorithmIds(algorithms);
        this.structuresById = indexStructures(structures);
        this.algorithmsById = indexAlgorithms(algorithms);
    }

    public List<StructureDescriptor> structures() {
        return List.copyOf(structuresById.values());
    }

    public List<AlgorithmDescriptor> algorithms() {
        return List.copyOf(algorithmsById.values());
    }

    public Optional<StructureDescriptor> findStructure(String id) {
        return Optional.ofNullable(structuresById.get(requireId(id)));
    }

    public StructureDescriptor requireStructure(String id) {
        String normalizedId = requireId(id);
        StructureDescriptor descriptor = structuresById.get(normalizedId);
        if (descriptor == null) {
            throw new IllegalArgumentException("No Structure registered for id: " + normalizedId);
        }
        return descriptor;
    }

    public Optional<AlgorithmDescriptor> findAlgorithm(String id) {
        return Optional.ofNullable(algorithmsById.get(requireId(id)));
    }

    public AlgorithmDescriptor requireAlgorithm(String id) {
        String normalizedId = requireId(id);
        AlgorithmDescriptor descriptor = algorithmsById.get(normalizedId);
        if (descriptor == null) {
            throw new IllegalArgumentException("No Algorithm registered for id: " + normalizedId);
        }
        return descriptor;
    }

    public AlgorithmDescriptor requireAlgorithm(String id, Class<?> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        AlgorithmDescriptor descriptor = requireAlgorithm(id);
        if (!descriptor.valueType().equals(valueType)) {
            throw new IllegalArgumentException("Algorithm " + id + " is registered for "
                    + descriptor.valueType().getName() + ", not " + valueType.getName());
        }
        return descriptor;
    }

    public List<AlgorithmDescriptor> algorithms(Class<?> valueType, Class<?> structureContract) {
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(structureContract, "structureContract");
        return algorithmsById.values().stream()
                .filter(descriptor -> descriptor.valueType().equals(valueType))
                .filter(descriptor -> descriptor.structureContract().equals(structureContract))
                .toList();
    }

    private static Map<String, StructureDescriptor> indexStructures(List<StructureDescriptor> descriptors) {
        LinkedHashMap<String, StructureDescriptor> indexed = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(java.util.Comparator.comparing(StructureDescriptor::id))
                .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<String, AlgorithmDescriptor> indexAlgorithms(List<AlgorithmDescriptor> descriptors) {
        LinkedHashMap<String, AlgorithmDescriptor> indexed = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(java.util.Comparator.comparing(AlgorithmDescriptor::id))
                .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return id;
    }
}
