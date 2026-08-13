package com.majortom.algorithms.app.leetcode.others;

public class 移除元素 {
    public static void main(String[] args) {
        Integer[] nums={0,1,2,2,3,0,4,2};
        System.out.println(removeElement(nums,2));
    }
    public static int removeElement(Integer[] nums, int val) {
        int n = nums.length;
        int left = 0;
        for (int right = 0; right < n; right++) {
            if (nums[right] != val) {
                nums[left] = nums[right];
                left++;
            }
        }
        return left;
    }
}
