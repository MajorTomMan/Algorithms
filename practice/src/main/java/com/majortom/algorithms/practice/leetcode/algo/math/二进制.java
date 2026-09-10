package com.majortom.algorithms.practice.leetcode.algo.math;



import com.majortom.algorithms.structure.linked.LinkedList;
import com.majortom.algorithms.structure.linked.StackStructure;



public class 二进制{
    public static void main(String[] args) {
        int i=16;
        StackStructure<Integer> stack=new LinkedList<>();
        while(i!=0){
            stack.push(i%2);
            i=i/2;
        }
        while(!stack.isEmpty()){
            System.out.print(stack.pop());
        }
    }
}
