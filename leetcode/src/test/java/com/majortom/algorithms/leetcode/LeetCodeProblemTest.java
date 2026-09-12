package com.majortom.algorithms.leetcode;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;
import com.majortom.algorithms.leetcode.algo.dp.爬楼梯;
import com.majortom.algorithms.leetcode.algo.search.搜索插入位置;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeetCodeProblemTest {
    @Test
    void formalProblemsUseOnlyCoreProblemMetadata() {
        assertProblem(搜索插入位置.class, "35");
        assertProblem(爬楼梯.class, "70");
    }

    @Test
    void formalProblemImplementationsRemainExecutableWithoutPracticeRuntime() {
        assertEquals(2, 搜索插入位置.BinarysearchInsert(new Integer[] {1, 3, 5, 6}, 5));
        assertEquals(8, 爬楼梯.climbStairs(5));
    }

    private static void assertProblem(Class<?> type, String id) {
        Problem problem = type.getAnnotation(Problem.class);
        assertEquals(ProblemSource.LEETCODE, problem.source());
        assertEquals(id, problem.id());
        long entries = Arrays.stream(type.getDeclaredMethods())
                .filter(LeetCodeProblemTest::isProblemEntry)
                .count();
        assertEquals(1L, entries);
    }

    private static boolean isProblemEntry(Method method) {
        return method.isAnnotationPresent(ProblemEntry.class);
    }
}
