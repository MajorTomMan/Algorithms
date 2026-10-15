package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;
import com.majortom.algorithms.structure.discovery.StructureDiscovery;
import java.util.List;

/** Batch discovery still validates every descriptor and rejects duplicate registrations. */
public final class RegistryDiscoveryChecks {
  private RegistryDiscoveryChecks() {}

  public static void main(String[] args) {
    ClassLoader loader = RegistryDiscoveryChecks.class.getClassLoader();
    var structures = new StructureDiscovery().discover(loader);
    var algorithms = new AlgorithmDiscovery().discover(loader);
    check(!structures.isEmpty() && !algorithms.isEmpty(), "discovery must find registered components");
    var registry = new ComponentRegistry(structures, algorithms);
    check(registry.structures().size() == structures.size(), "all structures registered");
    check(registry.algorithms().size() == algorithms.size(), "all algorithms registered");
    for (var structure : structures) {
      check(registry.requireStructure(structure.id()).equals(structure), "structure lookup stable");
    }
    for (var algorithm : algorithms) {
      check(registry.requireAlgorithm(algorithm.key()).equals(algorithm), "algorithm lookup stable");
    }
    expectDuplicate(() -> RegistrationValidator.validateUniqueStructureIds(
        List.of(structures.getFirst(), structures.getFirst())));
    expectDuplicate(() -> RegistrationValidator.validateUniqueAlgorithmKeys(
        List.of(algorithms.getFirst(), algorithms.getFirst())));
    System.out.println("RegistryDiscoveryChecks passed");
  }

  private static void expectDuplicate(Runnable action) {
    try {
      action.run();
    } catch (RegistrationException expected) {
      return;
    }
    throw new AssertionError("Duplicate registrations must be rejected");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
