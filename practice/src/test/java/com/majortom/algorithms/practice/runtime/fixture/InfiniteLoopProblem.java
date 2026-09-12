package com.majortom.algorithms.practice.runtime.fixture;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;

@Problem(source = ProblemSource.LOCAL, id = "infinite-loop-probe", name = "Infinite loop timeout probe")
public final class InfiniteLoopProblem {
    @ProblemEntry
    public static long loop() {
        long value = 0L;
        while (true) {
            value++;
        }
    }
}
