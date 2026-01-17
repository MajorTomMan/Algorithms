package com.majortom.algorithms.core.sort.alg;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

public class InsertionSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {
    /**
     * 插入排序：
     * 1. 从数组第二个元素开始，将其设为当前待排序的关键值(key)。
     * 2. 将 key 与其左侧已排序序列中的元素从右向左依次进行比较。
     * 3. 若左侧元素大于 key，则将该元素向右移动一个位置。
     * 4. 重复此过程，直到找到一个小于或等于 key 的元素，或者已经到达序列起始位置。
     * 5. 将 key 插入到该空位（j + 1）处。
     */
    @Override
    public void sort(BaseSort<T> sortEntity) {
        // TODO Auto-generated method stub
        T[] data = sortEntity.getData();
        for (int i = 1; i < data.length; i++) {
            T right = data[i];
            int j = i - 1;
            for (; j >= 0; j--) {
                T left = data[j];
                if (right.compareTo(left) < 0) {
                    data[j + 1] = left;
                    sync(sortEntity, i, j);
                } else {
                    break;
                }
            }
            data[j + 1] = right;
        }
    }

}
