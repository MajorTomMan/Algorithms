package com.majortom.algorithms.app.leetcode.algo.math;

import java.util.Scanner;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Stack;


public class 水仙花数 {
    public static void main(String[] args) {
        Scanner scanner =new Scanner(System.in);
        int n=scanner.nextInt();
        int m=scanner.nextInt();
        System.out.println(isNarcissistic(n,m));
        scanner.close();
    }
    public static String isNarcissistic(int n,int m){
        int temp=n;
        String s="";
        Integer[] array=new Integer[3];
        Stack<Integer> stack=new LinkedList<>();
        while(n<=m){
            if(temp!=0){
                stack.push(temp%10);
                temp=temp/10;
            }
            else{
                array[0]=stack.pop();
                array[1]=stack.pop();
                array[2]=stack.pop();
                if(Math.pow(array[0], 3)+Math.pow(array[1], 3)+Math.pow(array[2], 3)==n){
                    s+=array[0]+""+array[1]+""+array[2];
                    s+=" ";
                }
                n++;
                temp=n;
            }
        }
        if(s.length()==0){
            return "no";
        }else{
            return s;
        }
    }
}
