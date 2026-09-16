package com.majortom.algorithms.server.api.service.impl;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.server.api.constant.ExecutionState;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.error.AlgorithmNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionRejectedException;
import com.majortom.algorithms.server.api.service.AlgorithmExecutionService;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import com.majortom.algorithms.structure.array.ArrayStructure;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphStructure;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.structure.graph.WeightedGraphStructure;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.Maze;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.MazeStructure;
import com.majortom.algorithms.structure.string.StringStructure;

import jakarta.annotation.PreDestroy;

import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Service
@Log4j2
public class AlgorithmExecutionServiceImpl implements AlgorithmExecutionService {

    private static final String VERSION = "2.0";
    private static final int MAX_RETAINED_EXECUTIONS = 512;
    private static final ComponentRegistry COMPONENTS = ComponentDiscovery.discover();

    /**
     * Structure transport is selected only from @Algorithm.structure. No algorithm id, entry
     * signature, or algorithm category is inspected here.
     */
    private static final List<StructureInputFactory> STRUCTURE_INPUTS =
            List.of(
                    new ArrayInputFactory(),
                    new WeightedGraphInputFactory(),
                    new GraphInputFactory(),
                    new StringInputFactory(),
                    new MazeInputFactory());

    private final ObjectMapper objectMapper;
    private final ExecutionScheduler executionScheduler;
    private final Map<String, ExecutionUnit> executions = new ConcurrentHashMap<>();

    public AlgorithmExecutionServiceImpl(ObjectMapper objectMapper) {
        this(objectMapper, ExecutionScheduler.bounded("algorithm-executor-", 10, 20, 100));
    }

    AlgorithmExecutionServiceImpl(
            ObjectMapper objectMapper, ExecutionScheduler executionScheduler) {
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.executionScheduler =
                java.util.Objects.requireNonNull(executionScheduler, "executionScheduler");
    }

    @PreDestroy
    void closeScheduler() {
        executionScheduler.close();
    }

    @Override
    public String execute(ExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("execution request must not be null");
        }
        PreparedExecution prepared = prepareExecution(request.getAlgorithmId(), request.getInput());
        String runId = UUID.randomUUID().toString();
        ExecutionUnit unit = new ExecutionUnit();
        unit.setRunId(runId);
        unit.setAlgorithmId(request.getAlgorithmId());
        unit.setStatus(ExecutionState.QUEUED);
        unit.setCreatedAtEpochMillis(System.currentTimeMillis());
        executions.put(runId, unit);
        try {
            executionScheduler.execute(() -> runExecution(unit, prepared.operation()));
        } catch (RejectedExecutionException failure) {
            markRejected(unit, failure);
            pruneExecutions();
            throw new ExecutionRejectedException(runId, failure);
        }
        return runId;
    }

    private void runExecution(ExecutionUnit unit, ExecutionOperation<?> operation) {
        unit.setStatus(ExecutionState.RUNNING);
        log.info("Starting execution for runId: {}", unit.getRunId());
        RecordingEventSink eventSink = new RecordingEventSink();
        ExecutionRuntime runtime = new ExecutionRuntime(Clock.systemUTC(), unit::getRunId);
        try {
            ExecutionResult result = runtime.execute(unit.getAlgorithmId(), eventSink, operation);
            ExecutionRecording recording = eventSink.snapshot();
            applyRecording(unit, recording);
            result.output().ifPresent(unit::setResult);
            result.failure()
                    .ifPresent(
                            failure -> {
                                unit.setFailureCode(failure.code());
                                unit.setFailureMessage(failure.message());
                                unit.setFailureType(failure.exceptionType());
                            });
            switch (result.status()) {
                case COMPLETED -> unit.setStatus(ExecutionState.COMPLETED);
                case CANCELLED -> unit.setStatus(ExecutionState.CANCELLED);
                case FAILED -> unit.setStatus(ExecutionState.FAILED);
            }
        } catch (Throwable failure) {
            unit.setStatus(ExecutionState.FAILED);
            unit.setFailureCode("server.execution.unhandled");
            unit.setFailureMessage(message(failure));
            unit.setFailureType(failure.getClass().getName());
            unit.setDuration(
                    Math.max(0L, System.currentTimeMillis() - unit.getCreatedAtEpochMillis()));
            log.error("Unhandled execution failure for runId: {}", unit.getRunId(), failure);
        } finally {
            unit.setCompletedAtEpochMillis(System.currentTimeMillis());
            pruneExecutions();
        }
    }

    private void applyRecording(ExecutionUnit unit, ExecutionRecording recording) {
        unit.setDuration(recording.statistics().duration().toMillis());
        unit.setTotalEventCount(recording.statistics().totalEventCount());
        unit.setStatistics(recording.statistics().metrics());
        unit.setRecordingRunId(recording.runId());
        unit.setRecordingOperationId(recording.operationId());
    }

    private void markRejected(ExecutionUnit unit, Throwable failure) {
        unit.setStatus(ExecutionState.REJECTED);
        unit.setFailureCode("execution.scheduler.rejected");
        unit.setFailureMessage(message(failure));
        unit.setFailureType(failure.getClass().getName());
        unit.setCompletedAtEpochMillis(System.currentTimeMillis());
    }

    @Override
    public List<AlgorithmInformationDto> getAlgorithms() {
        return exposedAlgorithms().stream()
                .map(
                        descriptor -> {
                            AlgorithmInformationDto dto = new AlgorithmInformationDto();
                            dto.setId(descriptor.id());
                            dto.setName(descriptor.name());
                            dto.setModuleId(descriptor.module().id());
                            dto.setVersion(VERSION);
                            dto.setInputType(descriptor.structureContract().getName());
                            dto.setOutputType(descriptor.entryPoint().getReturnType().getName());
                            return dto;
                        })
                .toList();
    }

    @Override
    public ExecutionUnit getExecution(String runId) {
        ExecutionUnit unit = executions.get(runId);
        if (unit == null) {
            throw new ExecutionNotFoundException(runId);
        }
        return unit;
    }

    private PreparedExecution prepareExecution(String algorithmId, Map<String, Object> rawInput) {
        AlgorithmDescriptor descriptor = requireExposedAlgorithm(algorithmId);
        Map<String, Object> input = rawInput == null ? Map.of() : rawInput;
        StructureInputFactory factory =
                structureFactory(descriptor.structureContract())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Server has no input factory for structure "
                                                        + descriptor
                                                                .structureContract()
                                                                .getName()));
        Object structure = factory.create(input, objectMapper);
        return new PreparedExecution(() -> descriptor.invoke(structure));
    }

    private static Number number(Object value) {
        if (value instanceof Number number) return number;
        return Long.valueOf(java.lang.String.valueOf(value));
    }

    private static List<AlgorithmDescriptor> exposedAlgorithms() {
        return COMPONENTS.algorithms().stream()
                .filter(descriptor -> structureFactory(descriptor.structureContract()).isPresent())
                .sorted(
                        Comparator.comparing((AlgorithmDescriptor value) -> value.module().id())
                                .thenComparing(value -> value.valueType().getName())
                                .thenComparing(AlgorithmDescriptor::id))
                .toList();
    }

    private static AlgorithmDescriptor requireExposedAlgorithm(String algorithmId) {
        if (algorithmId == null || algorithmId.isBlank()) {
            throw new IllegalArgumentException("algorithmId must not be blank");
        }
        List<AlgorithmDescriptor> matches =
                exposedAlgorithms().stream()
                        .filter(candidate -> candidate.id().equals(algorithmId))
                        .toList();
        if (matches.isEmpty()) {
            throw new AlgorithmNotFoundException(algorithmId);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Algorithm id is ambiguous for the Server API: " + algorithmId);
        }
        return matches.getFirst();
    }

    private static Optional<StructureInputFactory> structureFactory(Class<?> structureContract) {
        return STRUCTURE_INPUTS.stream()
                .filter(factory -> factory.supports(structureContract))
                .findFirst();
    }

    private void pruneExecutions() {
        List<ExecutionUnit> terminal =
                executions.values().stream()
                        .filter(unit -> isTerminal(unit.getStatus()))
                        .sorted(Comparator.comparingLong(ExecutionUnit::getCompletedAtEpochMillis))
                        .toList();
        int excess = terminal.size() - MAX_RETAINED_EXECUTIONS;
        for (int index = 0; index < excess; index++) {
            ExecutionUnit unit = terminal.get(index);
            executions.remove(unit.getRunId(), unit);
        }
    }

    private boolean isTerminal(ExecutionState state) {
        return state == ExecutionState.COMPLETED
                || state == ExecutionState.CANCELLED
                || state == ExecutionState.FAILED
                || state == ExecutionState.REJECTED;
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private record PreparedExecution(ExecutionOperation<?> operation) {}

    private interface StructureInputFactory {
        Class<?> structureContract();

        Object create(Map<String, Object> input, ObjectMapper mapper);

        default boolean supports(Class<?> contract) {
            return structureContract().equals(contract);
        }
    }

    private static final class ArrayInputFactory implements StructureInputFactory {
        @Override
        public Class<?> structureContract() {
            return ArrayStructure.class;
        }

        @Override
        public Object create(Map<String, Object> input, ObjectMapper mapper) {
            Object rawValues = input.getOrDefault("values", List.of());
            @SuppressWarnings("unchecked")
            List<Object> values = mapper.convertValue(rawValues, List.class);
            return new com.majortom.algorithms.structure.array.Array<>(values);
        }
    }

    private static final class GraphInputFactory implements StructureInputFactory {
        @Override
        public Class<?> structureContract() {
            return GraphStructure.class;
        }

        @Override
        public Object create(Map<String, Object> input, ObjectMapper mapper) {
            Object rawGraph =
                    java.util.Objects.requireNonNull(input.get("graph"), "graph input is required");
            @SuppressWarnings("unchecked")
            GraphSnapshot<Integer> snapshot = mapper.convertValue(rawGraph, GraphSnapshot.class);
            return Graph.fromSnapshot(snapshot);
        }
    }

    private static final class WeightedGraphInputFactory implements StructureInputFactory {
        @Override
        public Class<?> structureContract() {
            return WeightedGraphStructure.class;
        }

        @Override
        public Object create(Map<String, Object> input, ObjectMapper mapper) {
            Object rawGraph =
                    java.util.Objects.requireNonNull(input.get("graph"), "graph input is required");
            @SuppressWarnings("unchecked")
            WeightedGraphSnapshot<Integer> snapshot =
                    mapper.convertValue(rawGraph, WeightedGraphSnapshot.class);
            return WeightedGraph.fromSnapshot(snapshot);
        }
    }

    private static final class StringInputFactory implements StructureInputFactory {
        @Override
        public Class<?> structureContract() {
            return StringStructure.class;
        }

        @Override
        public Object create(Map<String, Object> input, ObjectMapper mapper) {
            java.lang.String value =
                    mapper.convertValue(
                            input.getOrDefault("value", input.getOrDefault("target", "")),
                            java.lang.String.class);
            return new com.majortom.algorithms.structure.string.String(value);
        }
    }

    private static final class MazeInputFactory implements StructureInputFactory {
        @Override
        public Class<?> structureContract() {
            return MazeStructure.class;
        }

        @Override
        public Object create(Map<String, Object> input, ObjectMapper mapper) {
            Maze maze;
            if (input.containsKey("maze")) {
                GridMaze grid = mapper.convertValue(input.get("maze"), GridMaze.class);
                maze = new Maze(grid);
            } else {
                int rows = number(input.getOrDefault("rows", 51)).intValue();
                int columns = number(input.getOrDefault("columns", rows)).intValue();
                maze = new Maze(new MazeDimensions(rows, columns));
            }
            return maze;
        }
    }
}
