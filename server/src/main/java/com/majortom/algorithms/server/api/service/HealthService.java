package com.majortom.algorithms.server.api.service;

import java.util.List;

import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.dto.AlgoritmsInfomationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;

public interface HealthService {

    void execution(ExecutionRequest request);

    List<AlgoritmsInfomationDto> getAlgorithms();

    ExecutionUnit getExecution(String runId);
}
