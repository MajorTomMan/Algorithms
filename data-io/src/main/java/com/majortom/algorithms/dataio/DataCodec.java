package com.majortom.algorithms.dataio;

/** A format implementation that can read and write the same data type. */
public interface DataCodec<T> extends DataReader<T>, DataWriter<T> {
}
