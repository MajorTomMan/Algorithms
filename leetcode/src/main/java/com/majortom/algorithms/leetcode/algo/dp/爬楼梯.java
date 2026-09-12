package com.majortom.algorithms.leetcode.algo.dp;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;

@Problem(source = ProblemSource.LEETCODE, id = "70", name = "爬楼梯", number = "70", difficulty = com.majortom.algorithms.core.problem.ProblemDifficulty.EASY, tags = {"dynamic-programming"})
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
