package com.majortom.algorithms.app.leetcode.linear;

public class 数值的整数次方 {
    public static void main(String[] args) {
        System.out.println(myPow(2,10));
    }
    // 快速幂算法
    // 本质是分治算法
    // 解释在:https://leetcode.cn/problems/shu-zhi-de-zheng-shu-ci-fang-lcof/solution/mian-shi-ti-16-shu-zhi-de-zheng-shu-ci-fang-kuai-s/
    // >>右移运算符 将整个二进制串右移一位
    public static double myPow(double x, int n) {
        if(x == 0){
            return 0;
        }
        long b = n;
        double res = 1.0;
        if(b < 0) {
            x = 1 / x;
            b = -b;
        }
        while(b > 0) {
            if((b & 1) == 1){
                res *= x;
            }
            x *= x;
            b >>= 1;
        }
        return res;
    }
}
