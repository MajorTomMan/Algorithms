package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

public class HeapSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {

    @Override
    public void sort(BaseSort<T> sortEntity) {
        // TODO Auto-generated method stub
        T[] data = sortEntity.getData();
        int n = data.length;
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(sortEntity, n, i);
        }
        for (int i = n - 1; i > 0; i--) {
            swap(sortEntity, 0, i);
            heapify(sortEntity, i, 0);
        }
    }

    private void heapify(BaseSort<T> sortEntity, int n, int i) {
        T[] data = sortEntity.getData();
        int largest = i; // 先假设父节点是最大的
        int l = 2 * i + 1;
        int r = 2 * i + 2;

        // 如果左孩子在堆范围内，且比当前最大值还大
        if (l < n && data[l].compareTo(data[largest]) > 0) {
            largest = l;
        }

        // 如果右孩子在堆范围内，且比当前最大值还大
        if (r < n && data[r].compareTo(data[largest]) > 0) {
            largest = r;
        }

        // 如果发现最大值不是父节点，执行交换并“追责到底”
        if (largest != i) {
            swap(sortEntity, i, largest);
            // 关键：递归处理受影响的子树
            heapify(sortEntity, n, largest);
        }
    }

}
