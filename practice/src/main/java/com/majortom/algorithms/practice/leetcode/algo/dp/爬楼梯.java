package com.majortom.algorithms.practice.leetcode.algo.dp;

import com.majortom.algorithms.practice.runtime.annotation.Problem;
import com.majortom.algorithms.practice.runtime.annotation.ProblemEntry;
import com.majortom.algorithms.practice.runtime.model.ProblemSource;

@Problem(source = ProblemSource.LEETCODE, id = "70", title = "爬楼梯")
public class 爬楼梯 {
    public static void main(String[] args) {
        System.out.println(climbStairs(3));
    }
    @ProblemEntry
    public static int climbStairs(int n) {
        if(n<2){
            return 1;
        }
        return climbStairs(n-1)+climbStairs(n-2);
    }
}
