package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;

public class SelectionSort extends BaseSort {
    public void sort(int[] data) {
        int N = data.length;
        for (int i = 0; i < N; i++) {
            int min = i;
            for (int j = i + 1; j < N; j++) {
                if (less(data, data[j], data[min])) {
                    min = j;
                }
            }
            swap(data, i, min);
        }
    }
}
