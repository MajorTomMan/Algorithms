package com.majortom.algorithms.app.leetcode.linear;

public class 只出现一次的数字 {
    public static void main(String[] args) {
        Integer[] nums={4,1,2,1,2};
        System.out.println(singleNumber(nums));
    }
    public static int singleNumber(Integer[] nums) {
        int single=0;
        for(int num:nums){
            single^=num;
        }
        return single;
    }
}
