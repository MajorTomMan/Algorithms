package com.majortom.algorithms.server.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.server.dto.AlgoritmsInfomationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;

import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/v1")
@Log4j2
public class HealthController {
    private static final String HEALTH_CHECK = "OK";
    private ProviderCatalog providerCatalog;
    private final ObjectMapper objectMapper;

    public HealthController(ObjectMapper objectMapper) {
        this.providerCatalog = ProviderCatalog.production();
        this.objectMapper = objectMapper;
    }

    @GetMapping("health")
    public String getHealth() {
        return HEALTH_CHECK;
    }

    @GetMapping("algorithms")
    public List<AlgoritmsInfomationDto> getAlgorithms() {
        return providerCatalog.providers().stream().map(provider -> {
            AlgoritmsInfomationDto dto = new AlgoritmsInfomationDto();
            dto.setId(provider.metadata().id());
            dto.setModuleId(provider.metadata().moduleId());
            dto.setVersion(provider.metadata().version());
            dto.setInputType(provider.inputType().getName());
            dto.setOutputType(provider.outputType().getName());
            return dto;
        }).toList();
    }

    @PostMapping("executions")
    public String requestExecution(@RequestBody ExecutionRequest request) {
        AlgorithmProvider<?, ?> provider = providerCatalog.require(request.getAlgorithmId());

        AlgorithmInput algorithmInput = objectMapper.convertValue(request.getInput(), provider.inputType());

        RecordingEventSink eventSink = new RecordingEventSink();
        DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner();

        ExecutionResult result = runner.run(provider.invoker(), algorithmInput, eventSink);
        ExecutionRecording recording = eventSink.snapshot();

        log.info("Execution completed: runId={}, algorithmId={}, status={}, duration={}, events={}",
                recording.runId(),
                recording.algorithmId(),
                result.status(),
                recording.statistics().duration(),
                recording.statistics().totalEventCount());

        log.info("Execution output: {}", result.output().orElse(null));

        log.info("Execution statistics: {}", recording.statistics());

        for (ExecutionEvent event : recording.events()) {
            log.debug("Event #{}: {}", event.sequence(), event.payload());
        }
        return "Execution received";
    }

}
