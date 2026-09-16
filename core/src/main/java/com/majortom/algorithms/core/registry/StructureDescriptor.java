package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.metadata.StructureModule;
import java.util.Objects;

public record StructureDescriptor(
    String id, String name, StructureModule module, Class<?> contract, Class<?> implementation) {
  public StructureDescriptor {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(module, "module");
    Objects.requireNonNull(contract, "contract");
    Objects.requireNonNull(implementation, "implementation");
  }
}
