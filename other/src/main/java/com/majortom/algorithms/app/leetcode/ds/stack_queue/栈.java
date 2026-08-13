package com.majortom.algorithms.app.leetcode.ds.stack_queue;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.interfaces.Stack;
import com.majortom.algorithms.library.basic.AlgorithmsUtils;

public class 栈 {
    public static void main(String[] args) {
        Integer[] sortedArray = AlgorithmsUtils.nearlySortedArray(20, 19);
        Stack<Integer> stack = new LinkedList<>();
        stack.push(sortedArray);
        stack.foreach((v)->{
            System.out.println(v);
        });
        System.out.println(stack.pop());


    }
}