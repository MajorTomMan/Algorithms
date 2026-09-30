package com.majortom.algorithms.core.snapshot;

import java.util.Objects;

/** Hash-table snapshot plus independent key/value runtime type metadata. */
public record HashTableStateSnapshot<K, V>(
    String keyTypeName,
    String valueTypeName,
    HashTableSnapshot<K, V> table) {

  public HashTableStateSnapshot {
    if (keyTypeName == null || keyTypeName.isBlank()) {
      throw new IllegalArgumentException("keyTypeName must not be blank");
    }
    if (valueTypeName == null || valueTypeName.isBlank()) {
      throw new IllegalArgumentException("valueTypeName must not be blank");
    }
    table = Objects.requireNonNull(table, "table");
  }

  public static <K, V> HashTableStateSnapshot<K, V> of(
      Class<?> keyType, Class<?> valueType, HashTableSnapshot<K, V> table) {
    Objects.requireNonNull(keyType, "keyType");
    Objects.requireNonNull(valueType, "valueType");
    return new HashTableStateSnapshot<>(keyType.getName(), valueType.getName(), table);
  }

  public void requireTypes(Class<?> keyType, Class<?> valueType) {
    Objects.requireNonNull(keyType, "keyType");
    Objects.requireNonNull(valueType, "valueType");
    if (!keyType.getName().equals(keyTypeName) || !valueType.getName().equals(valueTypeName)) {
      throw new IllegalArgumentException(
          "hash snapshot type mismatch: expected "
              + keyType.getName() + " -> " + valueType.getName()
              + ", actual " + keyTypeName + " -> " + valueTypeName);
    }
  }
}
