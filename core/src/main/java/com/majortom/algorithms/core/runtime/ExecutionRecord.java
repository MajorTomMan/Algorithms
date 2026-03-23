package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.List;

public record ExecutionRecord<S extends BaseStructure<?>>(
        String moduleId,
        String algorithmId,
        String inputSignature,
        long createdAtMillis,
        ExecutionTimeline<S> timeline,
        ExecutionStatsSnapshot summary,
        List<ExecutionMessage> messages) {
}
