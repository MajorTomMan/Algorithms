package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;

public class ShellSort extends BaseSort {

    @Override
    public void sort(int[] data) {
        // TODO Auto-generated method stub
        int N = data.length;
        // TODO Auto-generated method stub

        int h = 1;
        while (h < N / 3) { // 影响因子 递增数列
            h = 3 * h + 1;
        }
        while (h >= 1) {
            for (int i = h; i < N; i++) {
                for (int j = i; j >= h && less(data[j], data[j - h]); j -= h) {
                    swap(data, j, j - h);
                    push(data, j, j - h);
                }
            }
            h = h / 3;
        }
    }
}
