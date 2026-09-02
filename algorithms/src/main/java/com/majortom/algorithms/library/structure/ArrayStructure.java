package com.majortom.algorithms.library.structure;

import java.util.List;

public interface ArrayStructure<T> {
    int size();
    T get(int index);
    void set(int index, T value);
    void insert(int index, T value);
    T remove(int index);
    void swap(int leftIndex, int rightIndex);
    List<T> raw();
}
