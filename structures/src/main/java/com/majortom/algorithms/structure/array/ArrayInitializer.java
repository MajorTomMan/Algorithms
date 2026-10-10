package com.majortom.algorithms.structure.array;

import com.majortom.algorithms.structure.initialization.StructureInitializer;
import java.util.Collection;
import java.util.Objects;

/** Applies array contents produced by any generator, parser or data reader. */
public final class ArrayInitializer<T>
    implements StructureInitializer<ArrayStructure<T>, Collection<? extends T>> {

  @Override
  public void initialize(ArrayStructure<T> array, Collection<? extends T> values) {
    Objects.requireNonNull(array, "array");
    Objects.requireNonNull(values, "values");
    array.initialize(values);
  }

  public Array<T> create(Collection<? extends T> values) {
    Array<T> array = new Array<>();
    initialize(array, values);
    return array;
  }
}
