package com.majortom.algorithms.server.request;

import lombok.Data;

import java.util.Map;

@Data
public class ExecutionRequest {
    private String algorithmId;
    private Map<String, Object> input;
}
