package com.majortom.algorithms.core.registry;

import java.util.Objects;

/** Stable runtime identity of an Algorithm registration. */
public record AlgorithmKey(
    Class<?> structureContract,
    AlgorithmTypeSignature typeSignature,
    String algorithmId) {

  public AlgorithmKey {
    structureContract = Objects.requireNonNull(structureContract, "structureContract");
    typeSignature = Objects.requireNonNull(typeSignature, "typeSignature");
    algorithmId = requireText(algorithmId, "algorithmId");
  }

  public static AlgorithmKey of(AlgorithmDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    return new AlgorithmKey(
        descriptor.structureContract(), descriptor.typeSignature(), descriptor.id());
  }

  private static String requireText(String value, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
