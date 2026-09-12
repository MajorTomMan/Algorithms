package com.majortom.algorithms.practice.runtime.fixture;

import com.majortom.algorithms.core.annotation.Problem;
import com.majortom.algorithms.core.annotation.ProblemEntry;
import com.majortom.algorithms.core.problem.ProblemSource;

@Problem(source = ProblemSource.LOCAL, id = "search-insert-probe")
public final class SearchInsertProblem {
    @ProblemEntry
    public static int searchInsert(Integer[] nums, int target) {
        int low = 0;
        int high = nums.length - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            if (target <= nums[middle]) {
                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }
}
