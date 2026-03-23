package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

public record ExecutionFrame<S extends BaseStructure<?>>(
        long index,
        long timestampOffsetMillis,
        String label,
        Object focusA,
        Object focusB,
        S snapshot,
        ExecutionStatsSnapshot stats) {
}
