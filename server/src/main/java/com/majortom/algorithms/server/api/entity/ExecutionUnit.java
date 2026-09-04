package com.majortom.algorithms.server.api.entity;

import com.majortom.algorithms.server.api.constant.ExecutionState;
import lombok.Data;

import java.util.Map;

/** Queryable server-side execution summary retained for a bounded period. */
@Data
public class ExecutionUnit {
    private String runId;
    private String algorithmId;
    private volatile ExecutionState status;
    private volatile Object result;
    private volatile String failureCode;
    private volatile String failureMessage;
    private volatile String failureType;
    private volatile long duration;
    private volatile long totalEventCount;
    private volatile Map<String, Long> statistics = Map.of();
    private volatile String recordingRunId;
    private volatile String recordingOperationId;
    private long createdAtEpochMillis;
    private volatile long completedAtEpochMillis;
}
