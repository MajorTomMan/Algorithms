package com.majortom.algorithms.app.leetcode.algo.math;


import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.core.interfaces.Stack;


public class 回文数 {
    public static void main(String[] args) {
        System.out.println(isPalindrome(-121));
    }
    public static boolean isPalindrome(int x) {
        Stack<String> stack =new LinkedList<>();
        Queue<String> queue=new LinkedList<>();
        if(x<0){
            return false;
        }
        else{
            isPalindrome(x,stack, queue);
        }
        while(!stack.isEmpty()){
            if(!stack.pop().equals(queue.poll())){
                return false;
            }
        }
        return true;
    }
    private static int isPalindrome(int x,Stack<String> stack,Queue<String> queue){
        if(x==0){
            return x;
        }
        else if(x<0){
        }
        stack.push(String.valueOf(x%10));
        queue.add(String.valueOf(x%10));
        isPalindrome(x/10,stack,queue);
        return x;
    }
}
