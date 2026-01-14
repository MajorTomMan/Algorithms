package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

public class SelectionSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {
    /*
     * 选择排序思路：
     * 1. 遍历：从头开始遍历整个数组，位置记为 i。
     * 2. 搜索：在 i 之后的剩余序列中，寻找绝对最小的元素。
     * 3. 标记：通过比较，不断更新最小值（minest）及其索引（minIndex）。
     * 4. 归位：每轮搜索结束后，将找到的最小值与当前 i 位置的元素进行交换。
     * 5. 结果：每一轮外层循环都能确定一个位置的最终正确元素。
     */
    @Override
    public void sort(BaseSort<T> sortEntity) {
        // TODO Auto-generated method stub
        T[] data = sortEntity.getData();
        for (int i = 0; i < data.length; i++) {
            T minest = data[i];
            int minestIndex = i;
            for (int j = i + 1; j < data.length; j++) {
                if (minest.compareTo(data[j]) > 0) {
                    minest = data[j];
                    minestIndex = j;
                }
            }
            swap(sortEntity, i, minestIndex);
        }
    }

}
