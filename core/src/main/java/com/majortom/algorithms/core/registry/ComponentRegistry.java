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

/**
 * 结构和算法的不可变注册目录，是发现阶段与 UI 查询阶段之间的稳定边界。
 *
 * <p>StructureDiscovery 提供结构实现描述符，AlgorithmDiscovery 提供算法描述符；
 * 构造注册表时统一校验唯一性和类型兼容性。注册键由结构契约、
 * 类型签名和算法 ID 共同决定，显示名称不参与匹配。
 *
 * <p>启动时反射/扫描一次，工作台切换元素类型时只进行查询；
 * {@link #createStructure(String, Class)} 创建新结构实例，不加载业务文件。
 * core 的注册目录不依赖 algorithms 与 JavaFX，避免反向模块依赖。
 */
public final class ComponentRegistry {
  private static final Comparator<AlgorithmDescriptor> ALGORITHM_ORDER =
      Comparator.comparing((AlgorithmDescriptor descriptor) -> descriptor.module().id())
          .thenComparing(descriptor -> descriptor.structureContract().getName())
          .thenComparing(descriptor -> descriptor.typeSignature().stableName())
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

  public List<AlgorithmDescriptor> algorithms(
      StructureModule module, AlgorithmTypeSignature typeSignature) {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(typeSignature, "typeSignature");
    return algorithmsByKey.values().stream()
        .filter(descriptor -> descriptor.module() == module)
        .filter(descriptor -> descriptor.typeSignature().equals(typeSignature))
        .toList();
  }

  public List<AlgorithmDescriptor> compatibleAlgorithms(
      Class<?> activeStructure, AlgorithmTypeSignature typeSignature) {
    Objects.requireNonNull(activeStructure, "activeStructure");
    Objects.requireNonNull(typeSignature, "typeSignature");
    return algorithmsByKey.values().stream()
        .filter(descriptor -> descriptor.typeSignature().equals(typeSignature))
        .filter(descriptor -> descriptor.structureContract().isAssignableFrom(activeStructure))
        .toList();
  }

  public List<AlgorithmTypeSignature> typeSignatures() {
    return algorithmsByKey.values().stream()
        .map(AlgorithmDescriptor::typeSignature)
        .distinct()
        .sorted(Comparator.comparing(AlgorithmTypeSignature::stableName))
        .toList();
  }

  public List<AlgorithmTypeSignature> algorithmTypeSignatures(StructureModule module) {
    Objects.requireNonNull(module, "module");
    return algorithmsByKey.values().stream()
        .filter(descriptor -> descriptor.module() == module)
        .map(AlgorithmDescriptor::typeSignature)
        .distinct()
        .sorted(Comparator.comparing(AlgorithmTypeSignature::stableName))
        .toList();
  }

  public List<String> algorithmIds(
      StructureModule module, AlgorithmTypeSignature typeSignature) {
    return algorithms(module, typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .sorted()
        .toList();
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
      StructureModule module, AlgorithmTypeSignature typeSignature, String algorithmId) {
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(typeSignature, "typeSignature");
    String id = requireId(algorithmId);
    List<AlgorithmDescriptor> matches = algorithms(module, typeSignature).stream()
        .filter(descriptor -> descriptor.id().equals(id))
        .toList();
    if (matches.size() != 1) {
      if (matches.isEmpty()) {
        throw new IllegalArgumentException("No Algorithm registered for module=" + module.id()
            + ", types=" + typeSignature + ", id=" + id);
      }
      throw new IllegalArgumentException("Algorithm registration is ambiguous for module="
          + module.id() + ", types=" + typeSignature + ", id=" + id + ": "
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
    return "structure=" + key.structureContract().getName()
        + ", types=" + key.typeSignature()
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
