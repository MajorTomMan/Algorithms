package com.majortom.algorithms.core.registry;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ComponentRegistry {
    private final Map<String, StructureDescriptor> structuresById;
    private final Map<String, AlgorithmDescriptor> algorithmsById;

    public ComponentRegistry(List<StructureDescriptor> structures, List<AlgorithmDescriptor> algorithms) {
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(algorithms, "algorithms");
        RegistrationValidator.validateUniqueStructureIds(structures);
        RegistrationValidator.validateUniqueAlgorithmIds(algorithms);
        validateValueTypeNames(algorithms);
        this.structuresById = indexStructures(structures);
        this.algorithmsById = indexAlgorithms(algorithms);
    }

    public List<StructureDescriptor> structures() {
        return List.copyOf(structuresById.values());
    }

    public List<AlgorithmDescriptor> algorithms() {
        return List.copyOf(algorithmsById.values());
    }

    public List<AlgorithmDescriptor> algorithms(String moduleId, Class<?> valueType) {
        requireId(moduleId);
        Objects.requireNonNull(valueType, "valueType");
        return algorithmsById.values().stream()
                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                .filter(descriptor -> descriptor.valueType().equals(valueType))
                .toList();
    }

    public List<Class<?>> valueTypes() {
        return algorithmsById.values().stream()
                .map(AlgorithmDescriptor::valueType)
                .distinct()
                .sorted(java.util.Comparator.comparing(Class::getName))
                .toList();
    }

    public List<String> valueTypeNames() {
        return valueTypes().stream().map(Class::getSimpleName).toList();
    }

    public List<String> algorithmValueTypes(String moduleId) {
        requireId(moduleId);
        return algorithmsById.values().stream()
                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                .map(descriptor -> descriptor.valueType().getSimpleName())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> algorithmIds(String moduleId, String valueTypeName) {
        requireId(moduleId);
        Objects.requireNonNull(valueTypeName, "valueTypeName");
        if (valueTypeName.isBlank()) {
            throw new IllegalArgumentException("valueTypeName must not be blank");
        }
        return algorithmsById.values().stream()
                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                .filter(descriptor -> descriptor.valueType().getSimpleName().equals(valueTypeName))
                .map(AlgorithmDescriptor::id)
                .sorted()
                .toList();
    }

    public boolean hasStructure(String id) {
        return structuresById.containsKey(requireId(id));
    }

    public boolean hasAlgorithm(String id) {
        return algorithmsById.containsKey(requireId(id));
    }

    public boolean hasAlgorithmModule(String moduleId) {
        requireId(moduleId);
        return algorithmsById.values().stream()
                .anyMatch(descriptor -> descriptor.moduleId().equals(moduleId));
    }

    public Optional<StructureDescriptor> findStructure(String id) {
        return Optional.ofNullable(structuresById.get(requireId(id)));
    }

    public StructureDescriptor requireStructure(String id) {
        String normalized = requireId(id);
        StructureDescriptor descriptor = structuresById.get(normalized);
        if (descriptor == null) {
            throw new IllegalArgumentException("No Structure registered for id: " + normalized);
        }
        return descriptor;
    }

    public Optional<AlgorithmDescriptor> findAlgorithm(String id) {
        return Optional.ofNullable(algorithmsById.get(requireId(id)));
    }

    public AlgorithmDescriptor requireAlgorithm(String id) {
        String normalized = requireId(id);
        AlgorithmDescriptor descriptor = algorithmsById.get(normalized);
        if (descriptor == null) {
            throw new IllegalArgumentException("No Algorithm registered for id: " + normalized);
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

    public <T> T createStructure(String id, Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        StructureDescriptor descriptor = requireStructure(id);
        if (!contract.isAssignableFrom(descriptor.implementation())) {
            throw new RegistrationException("Structure " + id + " implementation "
                    + descriptor.implementation().getName() + " is not assignable to " + contract.getName());
        }
        return contract.cast(instantiate(descriptor.implementation(), "Structure", id));
    }

    public <T> T createAlgorithm(String id, Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        AlgorithmDescriptor descriptor = requireAlgorithm(id);
        if (!contract.isAssignableFrom(descriptor.implementation())) {
            throw new RegistrationException("Algorithm " + id + " implementation "
                    + descriptor.implementation().getName() + " is not assignable to " + contract.getName());
        }
        return contract.cast(instantiate(descriptor.implementation(), "Algorithm", id));
    }

    private static Object instantiate(Class<?> implementation, String component, String id) {
        try {
            return implementation.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException exception) {
            throw new RegistrationException("Unable to instantiate " + component + " " + id
                    + " using " + implementation.getName(), exception);
        }
    }

    private static Map<String, StructureDescriptor> indexStructures(List<StructureDescriptor> descriptors) {
        LinkedHashMap<String, StructureDescriptor> indexed = new LinkedHashMap<>();
        descriptors.stream().sorted(java.util.Comparator.comparing(StructureDescriptor::id))
                .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<String, AlgorithmDescriptor> indexAlgorithms(List<AlgorithmDescriptor> descriptors) {
        LinkedHashMap<String, AlgorithmDescriptor> indexed = new LinkedHashMap<>();
        descriptors.stream().sorted(java.util.Comparator.comparing(AlgorithmDescriptor::id))
                .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static void validateValueTypeNames(List<AlgorithmDescriptor> descriptors) {
        Map<String, Class<?>> names = new LinkedHashMap<>();
        for (AlgorithmDescriptor descriptor : descriptors) {
            String simpleName = descriptor.valueType().getSimpleName();
            Class<?> previous = names.putIfAbsent(simpleName, descriptor.valueType());
            if (previous != null && !previous.equals(descriptor.valueType())) {
                throw new RegistrationException("Ambiguous algorithm value type name '" + simpleName
                        + "': " + previous.getName() + " vs " + descriptor.valueType().getName());
            }
        }
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return id;
    }
}
