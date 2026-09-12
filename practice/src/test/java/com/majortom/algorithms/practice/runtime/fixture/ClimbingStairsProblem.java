package com.majortom.algorithms.practice.runtime.fixture;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;

@Problem(source = ProblemSource.LOCAL, id = "climbing-stairs-probe", name = "Climbing stairs probe")
public final class ClimbingStairsProblem {
    @ProblemEntry
    public static int climbStairs(int n) {
        if (n < 2) {
            return 1;
        }
        return climbStairs(n - 1) + climbStairs(n - 2);
    }
}
