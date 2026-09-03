package com.majortom.algorithms.app.leetcode.algo.math;


import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;


public class 回文数 {
    public static void main(String[] args) {
        System.out.println(isPalindrome(-121));
    }
    public static boolean isPalindrome(int x) {
        StackStructure<String> stack =new LinkedList<>();
        QueueStructure<String> queue=new LinkedList<>();
        if(x<0){
            return false;
        }
        else{
            isPalindrome(x,stack, queue);
        }
        while(!stack.isEmpty()){
            if(!stack.pop().equals(queue.dequeue())){
                return false;
            }
        }
        return true;
    }
    private static int isPalindrome(int x,StackStructure<String> stack,QueueStructure<String> queue){
        if(x==0){
            return x;
        }
        else if(x<0){
        }
        stack.push(String.valueOf(x%10));
        queue.enqueue(String.valueOf(x%10));
        isPalindrome(x/10,stack,queue);
        return x;
    }
}
