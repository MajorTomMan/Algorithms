package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

/**
 * 快速排序实现
 * 适配说明：完全基于 BaseSortAlgorithms 的 less/swap，实现自动统计与步进动画。
 */
public class QuickSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {

    @Override
    public void sort(BaseSort<T> sortEntity) {
        if (sortEntity == null || sortEntity.getData() == null) {
            return;
        }

        // 1. 开始递归排序
        quickSort(sortEntity, 0, sortEntity.size() - 1);

        // 2. 排序完成后，利落地清除所有高亮焦点并最后同步一次
        sortEntity.reset(); // 或者使用 sortEntity.clearStatus();
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
        // 🚩 这里可以微调一下：将 high (Pivot) 的索引告知实体
        // 这样在 UI 上可以给 Pivot 一个特殊的颜色区分
        int i = low;

        for (int j = low; j < high; j++) {
            // less() 内部会处理 compareCount++ 并高亮 j 和 high
            if (less(sortEntity, j, high)) {
                // swap() 内部会处理 actionCount++ 并高亮 i 和 j
                swap(sortEntity, i, j);
                i++;
            }
        }

        // 将基准点交换到中间
        swap(sortEntity, i, high);
        return i;
    }
}