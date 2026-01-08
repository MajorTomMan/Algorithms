package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Stack;

public class 栈实现队列 {
    private static class StackQueue{
        private Stack<Integer> stack_1;
        private Stack<Integer> stack_2;
        public StackQueue(){
            stack_1=new LinkedList<>();
            stack_2=new LinkedList<>();
        }
        public void appendTail(int value) {
            stack_1.push(value);
        }
        
        public int deleteHead() {
            if(stack_2.isEmpty()){
                if(stack_1.isEmpty()){
                    return -1;
                }
                while(!stack_1.isEmpty()){
                    stack_2.push(stack_1.pop());
                }
            }
            return stack_2.pop();
        }
    }
    public static void main(String[] args) {
        StackQueue queue=new StackQueue();
        System.out.println(queue.deleteHead());
        queue.appendTail(5);
        queue.appendTail(2);
        System.out.println(queue.deleteHead());
        System.out.println(queue.deleteHead());
    }
}
