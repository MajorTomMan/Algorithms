package com.majortom.algorithms.visualization.runtime.value;

/** Converts editable Workbench text at the client boundary into a runtime value. */
public interface ValueAdapter<T> {
    Class<T> type();

    T parse(String text);

    String format(T value);
}
