package com.majortom.algorithms.app.leetcode.algo.math;



import java.util.Deque;
import java.util.LinkedList;



public class 二进制{
    public static void main(String[] args) {
        int i=16;
        Deque<Integer> stack=new LinkedList<>();
        while(i!=0){
            stack.push(i%2);
            i=i/2;
        }
        while(!stack.isEmpty()){
            System.out.print(stack.pop());
        }
    }
}
