
package com.majortom.algorithms.core.interfaces;

import java.util.Comparator;
import java.util.function.Consumer;

public interface List<T> extends Iterable<T> {

    boolean isEmpty();

    int size();

    void add(T t);

    void add(T[] t);

    T get(int index);

    void remove(int index);

    void replace(T t, int index);

    void foreach(Consumer<T> action);

    void sort(Comparator<T> comparator);

    void sort();

    void reverse();

    boolean contains(T t);
}