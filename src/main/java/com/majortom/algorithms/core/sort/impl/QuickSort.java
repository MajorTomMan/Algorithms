package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

/**
 * 快速排序实现
 * 适配实验室可视化架构：通过 less 和 swap 触发同步信号
 */
public class QuickSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {

    @Override
    public void sort(BaseSort<T> sortEntity) {
        if (sortEntity == null || sortEntity.getData() == null)
            return;

        T[] arr = sortEntity.getData();
        quickSort(sortEntity, 0, arr.length - 1);

        // 排序完成后，清除高亮焦点
        sortEntity.setActiveIndex(-1);
        sortEntity.setCompareIndex(-1);
        sync(sortEntity, -1, -1);
    }

    private void quickSort(BaseSort<T> sortEntity, int low, int high) {
        if (low < high) {
            // 获取分区索引
            int p = partition(sortEntity, low, high);

            // 递归排序左半部分
            quickSort(sortEntity, low, p - 1);
            // 递归排序右半部分
            quickSort(sortEntity, p + 1, high);
        }
    }

    private int partition(BaseSort<T> sortEntity, int low, int high) {
        // 选择最右侧元素作为基准点 (Pivot)
        // 也可以在这里加入随机选择基准点的逻辑
        int i = low;

        for (int j = low; j < high; j++) {
            // 使用基类 less 方法：会自动触发比较计数和 UI 高亮
            if (less(sortEntity, j, high)) {
                // 使用基类 swap 方法：会自动触发操作计数和 UI 动画同步
                swap(sortEntity, i, j);
                i++;
            }
        }

        // 最后将基准点交换到正确的位置
        swap(sortEntity, i, high);
        return i;
    }
}