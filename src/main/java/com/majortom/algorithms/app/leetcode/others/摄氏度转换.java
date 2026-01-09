package com.majortom.algorithms.app.leetcode.others;



public class 摄氏度转换{
    public static void main(String[] args) {
        double degree=100;
        System.out.println("华氏温度:"+degree+",摄氏温度:"+transform(degree));
    }
    private static double transform(double calsius){
        return (calsius-32)*5/9;
    }
}
