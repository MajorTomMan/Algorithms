package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;

public class SelectionSort extends BaseSort {
    public void sort(int[] a) {
        int N = a.length;
        for (int i = 0; i < N; i++) {
            int min = i;
            for (int j = i + 1; j < N; j++) {
                if (less(a[j], a[min])) {
                    min = j;
                }
            }
            swap(a, i, min);
        }
    }
}
