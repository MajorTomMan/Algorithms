package com.majortom.algorithms.visualization.runtime.value;

/** Immutable, evaluated field value for one animation frame. */
public record FieldView(String key, String label, String value, boolean summary, boolean error) {}
