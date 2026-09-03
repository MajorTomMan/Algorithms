package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import com.majortom.algorithms.library.basic.utils.AlgorithmsUtils;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;

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
