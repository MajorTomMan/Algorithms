package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 队列 {
    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<>();
        Integer[] sortedArray = AlgorithmsUtils.sortedArray(29, 28);
        queue.add(sortedArray);
        queue.foreach((v) -> {
            System.out.println(v);
        });
        System.out.println("-------------------------------------");
        queue.poll();
        queue.foreach((v) -> {
            System.out.println(v);
        });
    }
}
