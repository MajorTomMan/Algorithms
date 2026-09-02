package com.majortom.algorithms.server.api.controller;

import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.service.AlgorithmExecutionService;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1")
public class AlgorithmsController {
    private static final String HEALTH_CHECK = "OK";
    private final AlgorithmExecutionService executionService;

    public AlgorithmsController(AlgorithmExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping("health")
    public String getHealth() {
        return HEALTH_CHECK;
    }

    @GetMapping("algorithms")
    public List<AlgorithmInformationDto> getAlgorithms() {
        return executionService.getAlgorithms();
    }

    @PostMapping("executions")
    public String requestExecution(@RequestBody ExecutionRequest request) {
        return executionService.execute(request);
    }

    @GetMapping("executions/{runId}")
    public ExecutionUnit getExecution(@PathVariable String runId) {
        return executionService.getExecution(runId);
    }
}
