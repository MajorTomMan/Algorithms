package com.majortom.algorithms.algorithm.array.sort.impl;

import com.majortom.algorithms.algorithm.array.sort.AbstractIntegerSort;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.structure.array.ArrayStructure;

@Algorithm(id = "bubble-sort", name = "冒泡排序", type = Integer.class, structure = ArrayStructure.class)
public class IntegerBubbleSort extends AbstractIntegerSort {

    @Override
    public int compare(Integer left, Integer right) {
        // TODO Auto-generated method stubb
        return Integer.compare(left, right);

    }

    /*
     * 1. 冒泡排序是什么?
     * 
     * 
     * 因为每一轮外层循环，当前未排序部分里最大的元素会像气泡一样，一路交换到最右边
     * 
     * 
     * 
     * 
     * 2. 分解冒泡排序
     * 2.1 每一步要做什么?
     * 2.1.1 扫描未排序部分
     * 2.1.2 假设当前位置的元素就是最大的元素
     * 2.1.3 将最大的元素跟相邻的元素进行比较
     * 2.1.4 如果该位置的元素大于后续的元素,则将其和邻居交换位置
     * 2.1.5 然后继续扫描后续元素,不停留在该元素上
     * 2.1.6 每轮结束后最大的元素必然在最后
     * 
     * 2.1.7 外层循环用于控制当前比较的元素,减少比较的范围
     * 
     * 
     * 3. 优化
     * 3.1 由于冒泡排序的特点是每轮结束后最大的元素都在最后面,故当遍历到右边有序数列时,后续比较已不再具有意义,故可将其减少
     * 3.1.1 分解优化
     * 3.1.1.1 因为最右边可以视为最左边的倒序,故可以从数组的最后面开始减去最左边的当前元素的位置
     * 3.1.1.2 可将该优化视为每轮的数组长度减少已经排序的元素个数,从最右边(即最大值)开始减少
     */
    @AlgorithmEntry
    public void sort(ArrayStructure<Integer> array) {
        Log.d("size:" + array.size());
        for (int i = 0; i < array.size(); i++) {
            Log.d("i:" + i);
            for (int j = 0; j + 1 < (array.size() - i); j++) {
                Log.d("j:" + j + " j+1:" + (j + 1));
                if (compareAt(array, j, j + 1) > 0) {
                    swap(array, j + 1, j);
                }
            }
        }
    }

}
