package com.majortom.algorithms.server.api.service;

import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;

import java.util.List;

public interface AlgorithmExecutionService {
    String execute(ExecutionRequest request);
    List<AlgorithmInformationDto> getAlgorithms();
    ExecutionUnit getExecution(String runId);
}
