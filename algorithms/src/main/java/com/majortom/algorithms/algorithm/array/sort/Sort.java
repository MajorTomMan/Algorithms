package com.majortom.algorithms.algorithm.array.sort;

import com.majortom.algorithms.algorithm.array.ArrayAlgorithm;
import com.majortom.algorithms.structure.array.ArrayStructure;

public interface Sort<T> extends ArrayAlgorithm<T> {
    int compare(T left, T right);
    void sort(ArrayStructure<T> array);
}
