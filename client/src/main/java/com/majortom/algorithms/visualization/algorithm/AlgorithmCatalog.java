package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import java.util.List;

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
    StructureModule module = StructureModule.fromId(moduleId);
    return REGISTRY.algorithms().stream()
        .filter(descriptor -> descriptor.module() == module)
        .toList();
  }

  public static List<String> forWorkbenchModule(
      String moduleId, AlgorithmTypeSignature typeSignature) {
    return REGISTRY.algorithms(StructureModule.fromId(moduleId), typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }

  public static List<String> compatibleAlgorithms(
      Class<?> activeStructure, AlgorithmTypeSignature typeSignature) {
    return REGISTRY.compatibleAlgorithms(activeStructure, typeSignature).stream()
        .map(AlgorithmDescriptor::id)
        .distinct()
        .toList();
  }
}
