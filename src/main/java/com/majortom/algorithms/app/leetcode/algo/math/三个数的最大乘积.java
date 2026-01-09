package com.majortom.algorithms.app.leetcode.algo.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class 三个数的最大乘积 {
    public static void main(String[] args) {
        int[] nums = { -1,-2,-3,-4};
        System.out.println(maximumProduct(nums));
    }

    public static int maximumProduct(int[] nums) {
        List<Integer> list = Arrays.stream(nums).boxed().collect(Collectors.toList());
        if (nums.length == 3) {
            Collections.sort(list);
            return list.get(0) * list.get(1) * list.get(2);
        }
        Collections.sort(list);
        Collections.reverse(list);
        return list.get(0) * list.get(1) * list.get(2);
    }
}
