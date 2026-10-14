package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.structure.graph.MultiGraphStructure;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Structure-driven algorithm lookup for the workbench. */
public final class AlgorithmCatalog {
  private static final ComponentRegistry REGISTRY = ComponentDiscovery.discover();

  private AlgorithmCatalog() {}

  public static String name(String algorithmId) {
    List<AlgorithmDescriptor> matches = REGISTRY.algorithms().stream()
        .filter(descriptor -> descriptor.id().equals(algorithmId))
        .toList();
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("No Algorithm registered for id: " + algorithmId);
    }
    List<String> names = matches.stream().map(AlgorithmDescriptor::name).distinct().toList();
    if (names.size() != 1) {
      throw new IllegalArgumentException(
          "Algorithm id has multiple display names across registrations: "
              + algorithmId + " -> " + names);
    }
    return names.getFirst();
  }

  public static AlgorithmDescriptor descriptor(
      String moduleId, AlgorithmTypeSignature typeSignature, String algorithmId) {
    if (StructureIds.MULTI_GRAPH.equals(moduleId))
      return compatibleDescriptor(MultiGraphStructure.class, typeSignature, algorithmId);
    if (StructureIds.GRAPH.equals(moduleId))
      return compatibleDescriptor(GraphStructure.class, typeSignature, algorithmId);
    return REGISTRY.requireAlgorithm(
        StructureModule.fromId(moduleId), typeSignature, algorithmId);
  }

  public static AlgorithmDescriptor compatibleDescriptor(
      Class<?> activeStructure,
      AlgorithmTypeSignature typeSignature,
      String algorithmId) {
    List<AlgorithmDescriptor> matches =
        REGISTRY.compatibleAlgorithms(activeStructure, typeSignature).stream()
            .filter(descriptor -> descriptor.id().equals(algorithmId))
            .toList();
    if (matches.size() != 1) {
      throw new IllegalArgumentException(
          "Expected exactly one compatible Algorithm for structure="
              + activeStructure.getName()
              + ", types=" + typeSignature
              + ", id=" + algorithmId
              + ", found=" + matches.size());
    }
    return matches.getFirst();
  }

  public static String name(
      String moduleId, AlgorithmTypeSignature typeSignature, String algorithmId) {
    return descriptor(moduleId, typeSignature, algorithmId).name();
  }

  public static List<String> forWorkbenchModule(String moduleId) {
    return descriptorsForWorkbenchModule(moduleId).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  static List<AlgorithmDescriptor> descriptorsForWorkbenchModule(String moduleId) {
    if (StructureIds.MULTI_GRAPH.equals(moduleId))
      return REGISTRY.compatibleAlgorithms(
          MultiGraphStructure.class, AlgorithmTypeSignature.of(Object.class));
    if (StructureIds.GRAPH.equals(moduleId))
      return REGISTRY.algorithms().stream()
          .filter(descriptor -> descriptor.structureContract().isAssignableFrom(GraphStructure.class))
          .toList();
    StructureModule module = StructureModule.fromId(moduleId);
    return REGISTRY.algorithms().stream()
        .filter(descriptor -> descriptor.module() == module)
        .toList();
  }

  public static List<String> forWorkbenchModule(
      String moduleId, AlgorithmTypeSignature typeSignature) {
    if (StructureIds.MULTI_GRAPH.equals(moduleId))
      return compatibleAlgorithms(MultiGraphStructure.class, typeSignature);
    if (StructureIds.GRAPH.equals(moduleId))
      return compatibleAlgorithms(GraphStructure.class, typeSignature);
    return REGISTRY.algorithms(StructureModule.fromId(moduleId), typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  /**
   * MultiGraph<T> algorithms: exact T registrations plus algorithms declared for Object.
   * Object is an opt-in node-type wildcard for MultiGraph algorithms only, not for
   * ordinary Graph<T> or other structure families. An exact signature takes precedence.
   */
  public static List<String> multiGraphAlgorithms(Class<?> nodeType) {
    return multiGraphDescriptors(nodeType).stream()
        .map(AlgorithmDescriptor::id).toList();
  }

  public static AlgorithmDescriptor multiGraphDescriptor(Class<?> nodeType, String algorithmId) {
    return multiGraphDescriptors(nodeType).stream()
        .filter(descriptor -> descriptor.id().equals(algorithmId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "No MultiGraph algorithm '" + algorithmId + "' supports " + nodeType.getName()));
  }

  private static List<AlgorithmDescriptor> multiGraphDescriptors(Class<?> nodeType) {
    java.util.Objects.requireNonNull(nodeType, "nodeType");
    AlgorithmTypeSignature exact = AlgorithmTypeSignature.of(nodeType);
    AlgorithmTypeSignature generic = AlgorithmTypeSignature.of(Object.class);
    Map<String, AlgorithmDescriptor> algorithms = new LinkedHashMap<>();
    // Add generic implementations first; exact implementations replace by algorithm ID.
    for (AlgorithmDescriptor descriptor : REGISTRY.algorithms()) {
      if (descriptor.structureContract().isAssignableFrom(MultiGraphStructure.class)
          && descriptor.typeSignature().equals(generic))
        algorithms.put(descriptor.id(), descriptor);
    }
    for (AlgorithmDescriptor descriptor : REGISTRY.algorithms()) {
      if (descriptor.structureContract().isAssignableFrom(MultiGraphStructure.class)
          && descriptor.typeSignature().equals(exact))
        algorithms.put(descriptor.id(), descriptor);
    }
    return List.copyOf(algorithms.values());
  }

  public static List<String> compatibleAlgorithms(
      Class<?> activeStructure, AlgorithmTypeSignature typeSignature) {
    return REGISTRY.compatibleAlgorithms(activeStructure, typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }
}
