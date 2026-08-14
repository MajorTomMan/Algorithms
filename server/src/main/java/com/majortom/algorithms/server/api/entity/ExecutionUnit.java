package com.majortom.algorithms.server.api.entity;


import com.majortom.algorithms.server.api.constant.ExecutionState;

import lombok.Data;

@Data
public class ExecutionUnit{
    private String runId;
    private String algorithmId;
    private ExecutionState status;
    private long duration;
    private long totalEventCount;
}
