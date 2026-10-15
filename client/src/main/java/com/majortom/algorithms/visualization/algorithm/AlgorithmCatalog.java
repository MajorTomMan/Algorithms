package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.algorithm.maze.MazeAlgorithm;
import com.majortom.algorithms.algorithm.maze.MazeModel;
import com.majortom.algorithms.algorithm.maze.MazeRole;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.metadata.StructureIds;
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
    if (StructureIds.GRAPH.equals(moduleId))
      return graphDescriptor(typeSignature.primaryType(), algorithmId);
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

  /** Maze role/model filtering belongs to the shared algorithm catalog, not a second catalog. */
  public static List<String> mazeAlgorithms(MazeRole role, MazeModel model) {
    java.util.Objects.requireNonNull(role, "role");
    java.util.Objects.requireNonNull(model, "model");
    return descriptorsForWorkbenchModule(StructureIds.MAZE).stream()
        .filter(descriptor -> {
          MazeAlgorithm metadata = descriptor.implementation().getAnnotation(MazeAlgorithm.class);
          return metadata != null && metadata.role() == role && metadata.model() == model;
        })
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  public static List<String> forWorkbenchModule(String moduleId) {
    return descriptorsForWorkbenchModule(moduleId).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  static List<AlgorithmDescriptor> descriptorsForWorkbenchModule(String moduleId) {
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
    if (StructureIds.GRAPH.equals(moduleId))
      return graphAlgorithms(typeSignature.primaryType());
    return REGISTRY.algorithms(StructureModule.fromId(moduleId), typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  /** Exact node-type registrations override generic Object traversal implementations. */
  public static List<String> graphAlgorithms(Class<?> nodeType) {
    return graphDescriptors(nodeType).stream().map(AlgorithmDescriptor::id).toList();
  }

  public static AlgorithmDescriptor graphDescriptor(Class<?> nodeType, String algorithmId) {
    return graphDescriptors(nodeType).stream()
        .filter(descriptor -> descriptor.id().equals(algorithmId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "No Graph algorithm '" + algorithmId + "' supports " + nodeType.getName()));
  }

  private static List<AlgorithmDescriptor> graphDescriptors(Class<?> nodeType) {
    java.util.Objects.requireNonNull(nodeType, "nodeType");
    AlgorithmTypeSignature exact = AlgorithmTypeSignature.of(nodeType);
    AlgorithmTypeSignature generic = AlgorithmTypeSignature.of(Object.class);
    Map<String, AlgorithmDescriptor> algorithms = new LinkedHashMap<>();
    for (AlgorithmDescriptor descriptor : REGISTRY.algorithms()) {
      if (descriptor.structureContract().isAssignableFrom(GraphStructure.class)
          && descriptor.typeSignature().equals(generic))
        algorithms.put(descriptor.id(), descriptor);
    }
    for (AlgorithmDescriptor descriptor : REGISTRY.algorithms()) {
      if (descriptor.structureContract().isAssignableFrom(GraphStructure.class)
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
