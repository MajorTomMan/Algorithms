package com.majortom.algorithms.structure.initialization;

/** Applies a prepared data model to a structure without knowing where the data came from. */
@FunctionalInterface
public interface StructureInitializer<S, D> {
  void initialize(S structure, D data);
}
