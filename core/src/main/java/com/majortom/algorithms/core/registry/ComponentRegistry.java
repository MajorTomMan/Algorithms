package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.metadata.StructureModule;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ComponentRegistry {
  private static final Comparator<AlgorithmDescriptor> ALGORITHM_ORDER =
      Comparator.comparing((AlgorithmDescriptor descriptor) -> descriptor.module().id())
          .thenComparing(descriptor -> descriptor.structureContract().getName())
          .thenComparing(descriptor -> descriptor.valueType().getName())
          .thenComparing(AlgorithmDescriptor::id);

  private final Map<String, StructureDescriptor> structuresById;
  private final Map<AlgorithmKey, AlgorithmDescriptor> algorithmsByKey;

  public ComponentRegistry(
      List<StructureDescriptor> structures, List<AlgorithmDescriptor> algorithms) {
    Objects.requireNonNull(structures, "structures");
    Objects.requireNonNull(algorithms, "algorithms");
    RegistrationValidator.validateUniqueStructureIds(structures);
    RegistrationValidator.validateUniqueAlgorithmKeys(algorithms);
    this.structuresById = indexStructures(structures);
    this.algorithmsByKey = indexAlgorithms(algorithms);
  }

  public List<StructureDescriptor> structures() {
    return List.copyOf(structuresById.values());
  }

  public List<AlgorithmDescriptor> algorithms() {
    return List.copyOf(algorithmsByKey.values());
  }

  public List<AlgorithmDescriptor> algorithms(StructureModule module, Class<?> valueType) {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(valueType, "valueType");
    return algorithmsByKey.values()
        .stream()
        .filter(descriptor -> descriptor.module() == module)
        .filter(descriptor -> descriptor.valueType().equals(valueType))
        .toList();
  }

  public List<AlgorithmDescriptor> compatibleAlgorithms(
      Class<?> activeStructure, Class<?> valueType) {
    Objects.requireNonNull(activeStructure, "activeStructure");
    Objects.requireNonNull(valueType, "valueType");
    return algorithmsByKey.values()
        .stream()
        .filter(descriptor -> descriptor.valueType().equals(valueType))
        .filter(descriptor -> descriptor.structureContract().isAssignableFrom(activeStructure))
        .toList();
  }

  public List<Class<?>> valueTypes() {
    return algorithmsByKey.values()
        .stream()
        .map(AlgorithmDescriptor::valueType)
        .distinct()
        .sorted(Comparator.comparing(Class::getName))
        .toList();
  }

  /** Use fully qualified names only when the same module contains colliding simple names. */
  public List<String> algorithmValueTypes(StructureModule module) {
    Objects.requireNonNull(module, "module");
    List<Class<?>> types = algorithmsByKey.values().stream()
        .filter(descriptor -> descriptor.module() == module)
        .map(AlgorithmDescriptor::valueType).distinct().toList();
    return types.stream().map(type -> types.stream()
        .filter(other -> other.getSimpleName().equals(type.getSimpleName())).count() > 1
            ? type.getName() : type.getSimpleName()).sorted().toList();
  }

  /** Deprecated name-based access remains for old UI callers but never mixes colliding classes. */
  public List<String> algorithmIds(StructureModule module, String valueTypeName) {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(valueTypeName, "valueTypeName");
    if (valueTypeName.isBlank()) throw new IllegalArgumentException("valueTypeName must not be blank");
    List<Class<?>> matches = algorithmsByKey.values().stream()
        .filter(descriptor -> descriptor.module() == module)
        .map(AlgorithmDescriptor::valueType).distinct()
        .filter(type -> type.getName().equals(valueTypeName)
            || type.getSimpleName().equals(valueTypeName)).toList();
    if (matches.size() > 1) throw new IllegalArgumentException(
        "Ambiguous algorithm value type: " + valueTypeName);
    if (matches.isEmpty()) return List.of();
    return algorithms(module, matches.getFirst()).stream().map(AlgorithmDescriptor::id)
        .distinct().sorted().toList();
  }

  public boolean hasStructure(String id) {
    return structuresById.containsKey(requireId(id));
  }

  public boolean hasAlgorithm(AlgorithmKey key) {
    return algorithmsByKey.containsKey(Objects.requireNonNull(key, "key"));
  }

  public boolean hasAlgorithmModule(StructureModule module) {
    Objects.requireNonNull(module, "module");
    return algorithmsByKey.values().stream().anyMatch(descriptor -> descriptor.module() == module);
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

  public AlgorithmDescriptor requireAlgorithm(AlgorithmKey key) {
    AlgorithmKey normalized = Objects.requireNonNull(key, "key");
    AlgorithmDescriptor descriptor = algorithmsByKey.get(normalized);
    if (descriptor == null) {
      throw new IllegalArgumentException("No Algorithm registered for " + describe(normalized));
    }
    return descriptor;
  }

  public AlgorithmDescriptor requireAlgorithm(
      StructureModule module, Class<?> valueType, String algorithmId) {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(valueType, "valueType");
    String id = requireId(algorithmId);
    List<AlgorithmDescriptor> matches = algorithms(module, valueType)
                                            .stream()
                                            .filter(descriptor -> descriptor.id().equals(id))
                                            .toList();
    if (matches.size() != 1) {
      if (matches.isEmpty()) {
        throw new IllegalArgumentException("No Algorithm registered for module=" + module.id()
            + ", type=" + valueType.getName() + ", id=" + id);
      }
      throw new IllegalArgumentException("Algorithm registration is ambiguous for module="
          + module.id() + ", type=" + valueType.getName() + ", id=" + id + ": "
          + matches.stream().map(value -> value.structureContract().getName()).toList());
    }
    return matches.getFirst();
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

  private static Object instantiate(Class<?> implementation, String component, String id) {
    try {
      return implementation.getDeclaredConstructor().newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
        | InvocationTargetException exception) {
      throw new RegistrationException(
          "Unable to instantiate " + component + " " + id + " using " + implementation.getName(),
          exception);
    }
  }

  private static Map<String, StructureDescriptor> indexStructures(
      List<StructureDescriptor> descriptors) {
    LinkedHashMap<String, StructureDescriptor> indexed = new LinkedHashMap<>();
    descriptors.stream()
        .sorted(Comparator.comparing(StructureDescriptor::id))
        .forEach(descriptor -> indexed.put(descriptor.id(), descriptor));
    return Collections.unmodifiableMap(indexed);
  }

  private static Map<AlgorithmKey, AlgorithmDescriptor> indexAlgorithms(
      List<AlgorithmDescriptor> descriptors) {
    LinkedHashMap<AlgorithmKey, AlgorithmDescriptor> indexed = new LinkedHashMap<>();
    descriptors.stream()
        .sorted(ALGORITHM_ORDER)
        .forEach(descriptor -> indexed.put(descriptor.key(), descriptor));
    return Collections.unmodifiableMap(indexed);
  }

  private static String describe(AlgorithmKey key) {
    return "structure=" + key.structureContract().getName() + ", type=" + key.valueType().getName()
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
