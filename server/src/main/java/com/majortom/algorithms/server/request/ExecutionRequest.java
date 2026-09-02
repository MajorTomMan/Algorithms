package com.majortom.algorithms.server.request;

import java.util.Map;

import lombok.Data;

@Data
public class ExecutionRequest {
    private String algorithmId;
    private Map<String, Object> input;
}
