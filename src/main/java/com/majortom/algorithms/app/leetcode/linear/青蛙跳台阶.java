package com.majortom.algorithms.app.leetcode.linear;

public class 青蛙跳台阶 {
    public static void main(String[] args) {
        System.out.println(numways(7));
    }

    private static int numways(int n) {
        int a = 1, b = 1, sum;
        for(int i = 0; i < n; i++){
            sum = (a + b) % 1000000007;
            a = b;
            b = sum;
        }
        return a;
    }

}
