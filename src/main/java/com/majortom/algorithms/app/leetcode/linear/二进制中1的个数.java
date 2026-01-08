package com.majortom.algorithms.app.leetcode.linear;


public class 二进制中1的个数 {
    public static void main(String[] args) {
        System.out.println(hammingWeight(128));
    }
    // 用左移和或运算符去检查是否能对的上各个二进制位
    public static int hammingWeight(int n) {
        int sum=0;
        int i=0;
        while(i<32){
            System.out.println("左移结果:"+(1<<i));
            if((n&(1<<i))!=0){
                sum++;
            }
            System.out.println("与结果:"+(n&(1<<i)));
            i++;
        }
        return sum;
    }
}
