package com.majortom.algorithms.server.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.service.HealthService;
import com.majortom.algorithms.server.dto.AlgoritmsInfomationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/v1")
public class HealthController {
    private static final String HEALTH_CHECK = "OK";
    private static HealthService healthService;

    public HealthController(HealthService healthService) {
        HealthController.healthService = healthService;
    }

    @GetMapping("health")
    public String getHealth() {
        return HEALTH_CHECK;
    }

    @GetMapping("algorithms")
    public List<AlgoritmsInfomationDto> getAlgorithms() {
        return healthService.getAlgorithms();
    }

    @PostMapping("executions")
    public String requestExecution(@RequestBody ExecutionRequest request) {
        healthService.execution(request);
        return "Execution received";
    }

    @GetMapping("executions/{runId}")
    public ExecutionUnit getExecution(@PathVariable String runId) {
        return healthService.getExecution(runId);
    }

}
