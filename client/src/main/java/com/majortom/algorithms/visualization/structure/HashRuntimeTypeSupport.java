package com.majortom.algorithms.visualization.structure;

import java.util.List;

/** Runtime key/value type capability for the two-parameter Hash workbench. */
public interface HashRuntimeTypeSupport {
  Class<?> runtimeKeyType();

  Class<?> runtimeHashValueType();

  List<Class<?>> supportedKeyTypes();

  List<Class<?>> supportedHashValueTypes();

  boolean hasValues();

  void setRuntimeTypes(Class<?> keyType, Class<?> valueType);
}
