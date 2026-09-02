package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import com.majortom.algorithms.library.basic.AlgorithmsUtils;

public class 队列 {
    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<>();
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(29, 28);
        queue.addAll(Arrays.asList(sortedArray));
        queue.forEach((v) -> {
            System.out.println(v);
        });
        System.out.println("-------------------------------------");
        queue.poll();
        queue.forEach((v) -> {
            System.out.println(v);
        });
    }
}
