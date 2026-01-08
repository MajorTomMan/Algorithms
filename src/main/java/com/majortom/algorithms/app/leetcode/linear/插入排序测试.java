/*
 * @Date: 2024-07-13 16:08:30
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2024-07-13 19:51:02
 * @FilePath: \ALG\app\linear\插入排序测试.java
 * @Description: MajorTomMan @版权声明 保留文件所有权利
 */
package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 插入排序测试{
    public static void main(String[] args) {
        Integer[] arr = AlgorithmsUtils.randomArray(10, 100);
        sort(arr);
        AlgorithmsUtils.display(arr);
    }
    /* 插入排序 */
    private static void sort(Integer[] arr) {
        for (int i = 1; i < arr.length; i++) {
            for (int j = i; j >= 1; j--) {
                if (AlgorithmsUtils.less(arr[j-1], arr[j])) {
                    AlgorithmsUtils.swap(arr, j, j-1);
                }
            }
        }
    }
}
