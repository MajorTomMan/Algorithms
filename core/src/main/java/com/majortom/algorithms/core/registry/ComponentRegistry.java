package com.majortom.algorithms.core.registry;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ComponentRegistry {
    private static final Comparator<AlgorithmDescriptor> ALGORITHM_ORDER = Comparator
            .comparing(AlgorithmDescriptor::moduleId)
            .thenComparing(descriptor -> descriptor.valueType().getName())
            .thenComparing(AlgorithmDescriptor::id);

    private final Map<String, StructureDescriptor> structuresById;
    private final Map<AlgorithmKey, AlgorithmDescriptor> algorithmsByKey;

    public ComponentRegistry(List<StructureDescriptor> structures, List<AlgorithmDescriptor> algorithms) {
        Objects.requireNonNull(structures, "structures");
        Objects.requireNonNull(algorithms, "algorithms");
        RegistrationValidator.validateUniqueStructureIds(structures);
        RegistrationValidator.validateUniqueAlgorithmKeys(algorithms);
        validateValueTypeNamesByModule(algorithms);
        this.structuresById = indexStructures(structures);
        this.algorithmsByKey = indexAlgorithms(algorithms);
    }

    public List<StructureDescriptor> structures() {
        return List.copyOf(structuresById.values());
    }

    public List<AlgorithmDescriptor> algorithms() {
        return List.copyOf(algorithmsByKey.values());
    }

    public List<AlgorithmDescriptor> algorithms(String moduleId, Class<?> valueType) {
        requireId(moduleId);
        Objects.requireNonNull(valueType, "valueType");
        return algorithmsByKey.values().stream()
                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                .filter(descriptor -> descriptor.valueType().equals(valueType))
                .toList();
    }

    public List<Class<?>> valueTypes() {
        return algorithmsByKey.values().stream()
                .map(AlgorithmDescriptor::valueType)
                .distinct()
                .sorted(Comparator.comparing(Class::getName))
                .toList();
    }

    public List<String> valueTypeNames() {
        return valueTypes().stream().map(Class::getSimpleName).toList();
    }

    public List<String> algorithmValueTypes(String moduleId) {
        requireId(moduleId);
        return algorithmsByKey.values().stream()
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
        return algorithmsByKey.values().stream()
                .filter(descriptor -> descriptor.moduleId().equals(moduleId))
                .filter(descriptor -> descriptor.valueType().getSimpleName().equals(valueTypeName))
                .map(AlgorithmDescriptor::id)
                .sorted()
                .toList();
    }

    public boolean hasStructure(String id) {
        return structuresById.containsKey(requireId(id));
    }

    public boolean hasAlgorithm(AlgorithmKey key) {
        return algorithmsByKey.containsKey(Objects.requireNonNull(key, "key"));
    }

    public boolean hasAlgorithm(String moduleId, Class<?> valueType, String algorithmId) {
        return hasAlgorithm(key(moduleId, valueType, algorithmId));
    }

    public boolean hasAlgorithmModule(String moduleId) {
        requireId(moduleId);
        return algorithmsByKey.values().stream()
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

    public Optional<AlgorithmDescriptor> findAlgorithm(AlgorithmKey key) {
        return Optional.ofNullable(algorithmsByKey.get(Objects.requireNonNull(key, "key")));
    }

    public Optional<AlgorithmDescriptor> findAlgorithm(
            String moduleId, Class<?> valueType, String algorithmId) {
        return findAlgorithm(key(moduleId, valueType, algorithmId));
    }

    public AlgorithmDescriptor requireAlgorithm(AlgorithmKey key) {
        AlgorithmKey normalized = Objects.requireNonNull(key, "key");
        AlgorithmDescriptor descriptor = algorithmsByKey.get(normalized);
        if (descriptor == null) {
            throw new IllegalArgumentException("No Algorithm registered for " + describe(normalized));
        }
        return descriptor;
    }

    public AlgorithmDescriptor requireAlgorithm(
            String moduleId, Class<?> valueType, String algorithmId) {
        return requireAlgorithm(key(moduleId, valueType, algorithmId));
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

    public <T> T createAlgorithm(AlgorithmKey key, Class<T> contract) {
        Objects.requireNonNull(contract, "contract");
        AlgorithmDescriptor descriptor = requireAlgorithm(key);
        if (!contract.isAssignableFrom(descriptor.implementation())) {
            throw new RegistrationException("Algorithm " + describe(descriptor.key()) + " implementation "
                    + descriptor.implementation().getName() + " is not assignable to " + contract.getName());
        }
        return contract.cast(instantiate(descriptor.implementation(), "Algorithm", describe(descriptor.key())));
    }

    public <T> T createAlgorithm(
            String moduleId, Class<?> valueType, String algorithmId, Class<T> contract) {
        return createAlgorithm(key(moduleId, valueType, algorithmId), contract);
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
        descriptors.stream().sorted(Comparator.comparing(StructureDescriptor::id))
                .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<AlgorithmKey, AlgorithmDescriptor> indexAlgorithms(List<AlgorithmDescriptor> descriptors) {
        LinkedHashMap<AlgorithmKey, AlgorithmDescriptor> indexed = new LinkedHashMap<>();
        descriptors.stream().sorted(ALGORITHM_ORDER)
                .forEach(descriptor -> indexed.put(descriptor.key(), descriptor));
        return Collections.unmodifiableMap(indexed);
    }

    private static void validateValueTypeNamesByModule(List<AlgorithmDescriptor> descriptors) {
        Map<String, Class<?>> names = new LinkedHashMap<>();
        for (AlgorithmDescriptor descriptor : descriptors) {
            String simpleName = descriptor.valueType().getSimpleName();
            String lookupKey = descriptor.moduleId() + "\0" + simpleName;
            Class<?> previous = names.putIfAbsent(lookupKey, descriptor.valueType());
            if (previous != null && !previous.equals(descriptor.valueType())) {
                throw new RegistrationException("Ambiguous algorithm value type name '" + simpleName
                        + "' in module " + descriptor.moduleId() + ": "
                        + previous.getName() + " vs " + descriptor.valueType().getName());
            }
        }
    }

    private static AlgorithmKey key(String moduleId, Class<?> valueType, String algorithmId) {
        return new AlgorithmKey(requireId(moduleId), Objects.requireNonNull(valueType, "valueType"), requireId(algorithmId));
    }

    private static String describe(AlgorithmKey key) {
        return "module=" + key.moduleId()
                + ", type=" + key.valueType().getName()
                + ", id=" + key.algorithmId();
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return id;
    }
}
