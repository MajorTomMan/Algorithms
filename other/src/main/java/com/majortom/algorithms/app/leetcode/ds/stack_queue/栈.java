package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import java.util.Deque;
import java.util.LinkedList;
import com.majortom.algorithms.library.basic.AlgorithmsUtils;

public class 栈 {
    public static void main(String[] args) {
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(20, 19);
        Deque<Integer> stack = new LinkedList<>();
        for (Integer value : sortedArray) {
            stack.push(value);
        }
        stack.forEach((v)->{
            System.out.println(v);
        });
        System.out.println(stack.pop());


    }
}