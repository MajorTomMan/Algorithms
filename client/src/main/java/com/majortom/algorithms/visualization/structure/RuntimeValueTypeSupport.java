package com.majortom.algorithms.visualization.structure;

import java.util.List;

/** Capability for Structure workbenches whose runtime value type can change without changing topology UI. */
public interface RuntimeValueTypeSupport {
    Class<?> runtimeValueType();

    List<Class<?>> supportedValueTypes();

    void setRuntimeValueType(Class<?> valueType);
}
