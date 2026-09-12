package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.practice.runtime.annotation.Problem;
import com.majortom.algorithms.practice.runtime.annotation.ProblemEntry;
import com.majortom.algorithms.practice.runtime.model.ProblemSource;

@Problem(source = ProblemSource.LOCAL, id = "infinite-loop-probe", title = "Infinite loop timeout probe")
public final class InfiniteLoopProblem {
    @ProblemEntry
    public static long loop() {
        long value = 0L;
        while (true) {
            value++;
        }
    }
}
