package com.majortom.algorithms.server.api.service.impl;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.majortom.algorithms.core.api.AlgorithmInput;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.server.api.constant.ExecutionState;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.service.HealthService;
import com.majortom.algorithms.server.dto.AlgoritmsInfomationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import com.majortom.algorithms.server.utils.ThreadPoolUtils;

import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.ObjectMapper;

@Service
@Log4j2
public class HealthServiceImpl implements HealthService {

    private ProviderCatalog providerCatalog;
    private final ObjectMapper objectMapper;
    private final ThreadPoolUtils threadPoolUtils;
    private final Map<String, ExecutionUnit> executions = new ConcurrentHashMap<>();

    public HealthServiceImpl(ObjectMapper objectMapper, ThreadPoolUtils threadPoolUtils) {
        this.providerCatalog = ProviderCatalog.production();
        this.objectMapper = objectMapper;
        this.threadPoolUtils = threadPoolUtils;
    }

    // 负责将请求的算法执行任务放入线程池中执行，并在执行过程中更新ExecutionUnit的状态和统计信息
    @Override
    public void execution(ExecutionRequest request) {
        AlgorithmProvider<?, ?> provider = providerCatalog.require(request.getAlgorithmId());
        AlgorithmInput algorithmInput = objectMapper.convertValue(request.getInput(), provider.inputType());

        String runId = UUID.randomUUID().toString();

        ExecutionUnit unit = new ExecutionUnit();
        unit.setRunId(runId);
        unit.setAlgorithmId(request.getAlgorithmId());
        unit.setStatus(ExecutionState.QUEUED);
        executions.put(runId, unit);
        threadPoolUtils.execute(() -> runExecution(unit, provider, algorithmInput));
    }

    // 执行算法的核心方法，负责调用算法提供者的执行器，并记录执行结果和统计信息
    private void runExecution(ExecutionUnit unit, AlgorithmProvider<?, ?> provider, AlgorithmInput input) {
        unit.setStatus(ExecutionState.RUNNING);
        log.info("Starting execution for runId: {}", unit.getRunId());

        DefaultAlgorithmRunner runner = new DefaultAlgorithmRunner(Clock.systemUTC(),
                unit::getRunId);
        RecordingEventSink recordingEventSink = new RecordingEventSink();
        ExecutionRecording recording = recordingEventSink.snapshot();
        ExecutionResult result = runner.run(provider.invoker(), input, recordingEventSink);
        unit.setDuration(recording.statistics().duration().toMillis());
        unit.setTotalEventCount(recording.statistics().totalEventCount());
        switch (result.status()) {
            case COMPLETED -> unit.setStatus(ExecutionState.COMPLETED);
            case CANCELLED -> unit.setStatus(ExecutionState.CANCELLED);
            case FAILED -> unit.setStatus(ExecutionState.FAILED);
        }
    }

    // 获取所有算法提供者的信息，并将其转换为AlgoritmsInfomationDto对象的列表返回
    @Override
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

    @Override
    public ExecutionUnit getExecution(String runId) {
        ExecutionUnit unit = executions.get(runId);
        if (unit == null) {
            throw new IllegalArgumentException("No execution found for runId: " + runId);
        }
        return unit;
    }

}
