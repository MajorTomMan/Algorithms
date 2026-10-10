package com.majortom.algorithms.dataio;

import java.io.IOException;
import java.io.OutputStream;

/** Serializes data to a caller-owned output stream without changing the source object. */
@FunctionalInterface
public interface DataWriter<T> {
  void write(T value, OutputStream output) throws IOException;
}
