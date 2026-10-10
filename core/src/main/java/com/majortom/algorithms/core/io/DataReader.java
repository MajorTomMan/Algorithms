package com.majortom.algorithms.core.io;

import java.io.IOException;
import java.io.InputStream;

/** Parses data from a caller-owned input stream without applying it to any structure. */
@FunctionalInterface
public interface DataReader<T> {
  T read(InputStream input) throws IOException;
}
