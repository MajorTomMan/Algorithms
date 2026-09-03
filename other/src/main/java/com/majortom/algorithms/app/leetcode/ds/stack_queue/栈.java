package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import com.majortom.algorithms.library.basic.AlgorithmsUtils;
import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.StackStructure;

public class 栈 {
    public static void main(String[] args) {
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(20, 19);
        StackStructure<Integer> stack = new LinkedList<>();
        for (Integer value : sortedArray) {
            stack.push(value);
        }
        for (Integer value : stack) {
            System.out.println(value);
        }
        System.out.println(stack.pop());
    }
}
