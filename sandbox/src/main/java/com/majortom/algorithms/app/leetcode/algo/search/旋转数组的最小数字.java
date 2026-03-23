package com.majortom.algorithms.app.leetcode.algo.search;

public class 旋转数组的最小数字 {
    public static void main(String[] args) {
        Integer[] nums={2,2,2,0,1};
        System.out.println(minArray(nums));
    }

    // 要有数学中的区间的概念,先找出中位数.然后根据要找的数在中位数哪侧或者等于中位数的情况做处理
    // 本质是逐渐逼近最小数所在的位置的算法,通过不断的判断条件来缩减区间所含元素来确定最小数所在位置
    // 当中位数大于右侧数时,代表最小数在右侧,故左侧数可以不管,直接忽略左区间,再进行下一次判断
    // 当中位数小于右侧数时,代表最小数在左侧或中位数,则右侧数可以不管,直接忽略右区间,再进行下一次判断
    public static int minArray(Integer[] numbers) {
        int low = 0;
        int high = numbers.length - 1;
        while (low < high) {
            int pivot = low + (high - low) / 2;
            if (numbers[pivot] < numbers[high]) {
                high = pivot;
            } else if (numbers[pivot] > numbers[high]) {
                low = pivot + 1;
            } else {
                high -= 1;
            }
        }
        return numbers[low];
    }
}
