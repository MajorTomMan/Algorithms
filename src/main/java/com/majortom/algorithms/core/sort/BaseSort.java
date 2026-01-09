package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseAlgorithm;

public abstract class BaseSort extends BaseAlgorithm<int[]> {

    public abstract void sort(int[] arr);

    protected void swap(int[] arr, int i, int j) {
        if (i == j)
            return;
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;

        actionCount++; // 统一使用 actionCount
        sync(arr, i, j); // 传入数组和当前的两个焦点索引
    }

    protected boolean less(int[] arr, int i, int j) {
        compareCount++; // 统一使用 compareCount
        sync(arr, i, j); // 比较时也同步一下，让 UI 亮起这两个柱子
        return arr[i] < arr[j];
    }
}