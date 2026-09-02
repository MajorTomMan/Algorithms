package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.library.structure.ArrayStructure;

public interface Sort<T> {
    int compare(T left, T right);
    void sort(ArrayStructure<T> array);
}
