package com.majortom.algorithms.practice.leetcode.ds.stack_queue;

import com.majortom.algorithms.practice.support.AlgorithmsUtils;
import com.majortom.algorithms.structure.linked.LinkedList;
import com.majortom.algorithms.structure.linked.QueueStructure;

public class 队列 {
    public static void main(String[] args) {
        QueueStructure<Integer> queue = new LinkedList<>();
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(29, 28);
        for (Integer value : sortedArray) {
            queue.enqueue(value);
        }
        for (Integer value : queue) {
            System.out.println(value);
        }
        System.out.println("-------------------------------------");
        queue.dequeue();
        for (Integer value : queue) {
            System.out.println(value);
        }
    }
}
