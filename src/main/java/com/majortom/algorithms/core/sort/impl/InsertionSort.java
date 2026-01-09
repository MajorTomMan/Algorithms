package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;

public class InsertionSort extends BaseSort {

    @Override
    public void sort(int[] data) {
        // TODO Auto-generated method stub
        int N = data.length;
        for (int i = 1; i < N; i++) {
            for (int j = i; j > 0 && less(data, data[j], data[j - 1]); j--) {
                swap(data, j, j - 1);
                sync(data, j, j - 1);
            }
        }
    }

}
