package com.majortom.algorithms.leetcode.algo.search;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;

@Problem(source = ProblemSource.LEETCODE, id = "35", name = "搜索插入位置", number = "35", difficulty = com.majortom.algorithms.core.problem.ProblemDifficulty.EASY, tags = {"array", "binary-search"})
public class 搜索插入位置 {
    public static void main(String[] args) {
        Integer[] nums = {1,3,5,6};
        System.out.println(BinarysearchInsert(nums, 5));
    }

    public static int searchInsert(Integer[] nums, int target) {
        if (target <= nums[0]) {
            return 0;
        }
        for (int i = 1; i < nums.length; i++) {
            if (target <= nums[i]) {
                return i;
            }
        }
        return nums.length;
    }
    @ProblemEntry
    public static int BinarysearchInsert(Integer[] nums, int target){
        int lo=0;
        int hi=nums.length-1;
        while(lo<=hi){
            int mid=lo+(hi-lo)/2;
            if(target<=nums[mid]){
                hi=mid-1;
            }
            else if(target>nums[mid]){
                lo=mid+1;
            }
        }
        return lo;
    }
}
