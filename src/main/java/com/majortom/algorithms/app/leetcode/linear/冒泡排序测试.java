package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 冒泡排序测试 {
    public static void main(String[] args) {
        Integer[] arr = AlgorithmsUtils.randomArray(10, 100);
        arr = sort(arr);
        AlgorithmsUtils.display(arr);
    }

    /* 冒泡排序 */
    private static Integer[] sort(Integer[] arr) {
        for (Integer i = 0; i < arr.length; i++) {
            boolean swapped = false;
            for (Integer j = i + 1; j < arr.length; j++) {
                if (AlgorithmsUtils.less(arr[i], arr[j])) {
                    AlgorithmsUtils.swap(arr, i, j);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
        return arr;
    }
}
