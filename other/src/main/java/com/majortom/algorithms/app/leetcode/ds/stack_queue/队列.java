package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.interfaces.Queue;
import com.majortom.algorithms.library.basic.AlgorithmsUtils;

public class 队列 {
    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<>();
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(29, 28);
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
