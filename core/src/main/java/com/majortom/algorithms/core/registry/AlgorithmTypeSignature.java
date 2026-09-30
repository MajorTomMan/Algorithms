package com.majortom.algorithms.core.registry;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Ordered runtime type signature of an Algorithm registration. */
public record AlgorithmTypeSignature(List<Class<?>> types) {
  public AlgorithmTypeSignature {
    types = List.copyOf(Objects.requireNonNull(types, "types"));
    if (types.isEmpty()) {
      throw new IllegalArgumentException("Algorithm type signature must not be empty");
    }
    for (Class<?> type : types) {
      Objects.requireNonNull(type, "type");
    }
  }

  public static AlgorithmTypeSignature of(Class<?>... types) {
    Objects.requireNonNull(types, "types");
    return new AlgorithmTypeSignature(Arrays.asList(types));
  }

  public int arity() {
    return types.size();
  }

  public Class<?> type(int index) {
    return types.get(index);
  }

  public Class<?> primaryType() {
    return types.getFirst();
  }

  public String stableName() {
    return types.stream().map(Class::getName).collect(Collectors.joining(","));
  }

  @Override
  public String toString() {
    return "[" + stableName() + "]";
  }
}
