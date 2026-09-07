package com.majortom.algorithms.library.sort;

import com.majortom.algorithms.library.array.ArrayAlgorithm;
import com.majortom.algorithms.library.structure.ArrayStructure;

public interface Sort<T> extends ArrayAlgorithm<T> {
    int compare(T left, T right);
    void sort(ArrayStructure<T> array);
}
