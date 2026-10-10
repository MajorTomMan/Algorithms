package com.majortom.algorithms.structure.initialization;

/** Produces initialization data independently of structure mutation or file I/O. */
@FunctionalInterface
public interface DataGenerator<D> {
  D generate();
}
