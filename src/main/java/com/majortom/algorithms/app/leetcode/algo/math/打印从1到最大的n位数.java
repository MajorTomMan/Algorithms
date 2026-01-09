package com.majortom.algorithms.app.leetcode.algo.math;

public class 打印从1到最大的n位数 {
    public static void main(String[] args) {
        Integer[] nums=printNumbers(3);
        for(int data:nums){
            System.out.print(data+" ");
        }
    }

    public static Integer[] printNumbers(int n) {
        if(n==0){
            return new Integer[1];
        }
        String s="";
        int i=0;
        while(i!=n){
            s+=""+9;
            i++;
        }
        int times=Integer.parseInt(s);
        Integer[] result=new Integer[times];
        for(i=1;i<=times;i++){
            result[i-1]=i;
        }
        return result;
    }
}
