package com.majortom.algorithms.core.sort.alg;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

/**
 * 快速排序实现
 */
public class QuickSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {

    @Override
    public void sort(BaseSort<T> sortEntity) {
        if (sortEntity == null || sortEntity.getData() == null) {
            return;
        }

        quickSort(sortEntity, 0, sortEntity.size() - 1);

        sortEntity.resetStatistics(); 
        sync(sortEntity, -1, -1);
    }

    private void quickSort(BaseSort<T> sortEntity, int low, int high) {
        // 检查线程中断，确保 UI 能够随时停止算法
        if (Thread.currentThread().isInterrupted())
            return;

        if (low < high) {
            int p = partition(sortEntity, low, high);

            quickSort(sortEntity, low, p - 1);
            quickSort(sortEntity, p + 1, high);
        }
    }

    private int partition(BaseSort<T> sortEntity, int low, int high) {
        int i = low;

        for (int j = low; j < high; j++) {
            if (less(sortEntity, j, high)) {
                swap(sortEntity, i, j);
                i++;
            }
        }

        swap(sortEntity, i, high);
        return i;
    }
}