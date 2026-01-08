package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.app.visualization.listener.SortListener;

public abstract class BaseSort {
    protected SortListener listener;

    // 计数器：记录比较和交换的次数
    protected int compareCount = 0;
    protected int swapCount = 0;

    public BaseSort setListener(SortListener listener) {
        this.listener = listener;
        return this; // 返回自己，支持链式操作
    }

    // 重置计数器（每次开始新排序前调用）
    public void resetStatistics() {
        this.compareCount = 0;
        this.swapCount = 0;
    }

    // 封装比较逻辑：每次调用都会自动计数
    protected boolean less(int a, int b) {
        compareCount++;
        return a < b;
    }

    // 封装交换逻辑：每次调用都会自动计数
    protected void swap(int[] arr, int i, int j) {
        if (i == j)
            return;
        swapCount++;
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // 此时的 push 需要把计数值也传出去
    protected void push(int[] data, int a, int b) {
        if (listener != null) {
            listener.onStep(data, a, b, compareCount, swapCount);
        }
    }

    public abstract void sort(int[] data);
}