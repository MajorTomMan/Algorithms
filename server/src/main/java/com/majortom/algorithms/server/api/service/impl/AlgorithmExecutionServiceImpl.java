package com.majortom.algorithms.server.api.service.impl;

import com.majortom.algorithms.algorithm.array.sort.Sort;
import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.algorithm.graph.GraphTraversal;
import com.majortom.algorithms.algorithm.maze.ArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.ArrayMazePathfinder;
import com.majortom.algorithms.algorithm.maze.GraphMazeGenerator;
import com.majortom.algorithms.algorithm.string.StringSearch;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.server.api.constant.ExecutionState;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.error.AlgorithmNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionRejectedException;
import com.majortom.algorithms.server.api.service.AlgorithmExecutionService;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import com.majortom.algorithms.server.request.GraphBfsRequest;
import com.majortom.algorithms.server.request.IntegerSortRequest;
import com.majortom.algorithms.server.request.MazeGenerationRequest;
import com.majortom.algorithms.server.request.MazePathRequest;
import com.majortom.algorithms.server.request.StringSearchRequest;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.maze.GridMaze;
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
     * Server transport protocols. This list describes API request/output contracts, not Algorithm identities.
     * Algorithm identity and metadata come exclusively from ComponentRegistry/AlgorithmDescriptor.
     */
    private static final List<ApiExecutionHandler> API_PROTOCOLS = List.of(
            new IntegerSortHandler(),
            new ArrayMazeGeneratorHandler(),
            new GraphMazeGeneratorHandler(),
            new ArrayMazePathfinderHandler(),
            new GraphTraversalHandler(),
            new StringSearchHandler());

    private final ObjectMapper objectMapper;
    private final ExecutionScheduler executionScheduler;
    private final Map<String, ExecutionUnit> executions = new ConcurrentHashMap<>();

    public AlgorithmExecutionServiceImpl(ObjectMapper objectMapper) {
        this(objectMapper, ExecutionScheduler.bounded("algorithm-executor-", 10, 20, 100));
    }

    AlgorithmExecutionServiceImpl(ObjectMapper objectMapper, ExecutionScheduler executionScheduler) {
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.executionScheduler = java.util.Objects.requireNonNull(executionScheduler, "executionScheduler");
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
            result.failure().ifPresent(failure -> {
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
            unit.setDuration(Math.max(0L, System.currentTimeMillis() - unit.getCreatedAtEpochMillis()));
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
        return exposedAlgorithms().stream().map(exposed -> {
            AlgorithmInformationDto dto = new AlgorithmInformationDto();
            dto.setId(exposed.descriptor().id());
            dto.setName(exposed.descriptor().name());
            dto.setModuleId(exposed.descriptor().moduleId());
            dto.setVersion(VERSION);
            dto.setInputType(exposed.handler().inputType().getName());
            dto.setOutputType(exposed.handler().outputType().getName());
            return dto;
        }).toList();
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
        ExposedAlgorithm exposed = requireExposedAlgorithm(algorithmId);
        Map<String, Object> input = rawInput == null ? Map.of() : rawInput;
        return exposed.handler().prepare(exposed.descriptor(), input, objectMapper);
    }

    private static List<ExposedAlgorithm> exposedAlgorithms() {
        return COMPONENTS.algorithms().stream()
                .map(descriptor -> handlerFor(descriptor)
                        .map(handler -> new ExposedAlgorithm(descriptor, handler)))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparing((ExposedAlgorithm value) -> value.descriptor().moduleId())
                        .thenComparing(value -> value.descriptor().valueType().getName())
                        .thenComparing(value -> value.descriptor().id()))
                .toList();
    }

    private static ExposedAlgorithm requireExposedAlgorithm(String algorithmId) {
        if (algorithmId == null || algorithmId.isBlank()) {
            throw new IllegalArgumentException("algorithmId must not be blank");
        }
        List<ExposedAlgorithm> matches = exposedAlgorithms().stream()
                .filter(candidate -> candidate.descriptor().id().equals(algorithmId))
                .toList();
        if (matches.isEmpty()) {
            throw new AlgorithmNotFoundException(algorithmId);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Algorithm id is ambiguous for the Server API: " + algorithmId);
        }
        return matches.getFirst();
    }

    private static Optional<ApiExecutionHandler> handlerFor(AlgorithmDescriptor descriptor) {
        return API_PROTOCOLS.stream().filter(handler -> handler.supports(descriptor)).findFirst();
    }

    private static <T> T createAlgorithm(AlgorithmDescriptor descriptor, Class<T> contract) {
        return COMPONENTS.createAlgorithm(descriptor.key(), contract);
    }

    private void pruneExecutions() {
        List<ExecutionUnit> terminal = executions.values().stream()
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
        return state == ExecutionState.COMPLETED || state == ExecutionState.CANCELLED
                || state == ExecutionState.FAILED || state == ExecutionState.REJECTED;
    }

    private static String message(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }

    private record PreparedExecution(ExecutionOperation<?> operation) {
    }

    private record ExposedAlgorithm(AlgorithmDescriptor descriptor, ApiExecutionHandler handler) {
    }

    private interface ApiExecutionHandler {
        boolean supports(AlgorithmDescriptor descriptor);
        Class<?> inputType();
        Class<?> outputType();
        PreparedExecution prepare(AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper);
    }

    private static final class IntegerSortHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(Integer.class)
                    && Sort.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return IntegerSortRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return List.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            IntegerSortRequest request = mapper.convertValue(input, IntegerSortRequest.class);
            @SuppressWarnings("unchecked")
            Sort<Integer> algorithm = (Sort<Integer>) createAlgorithm(descriptor, Sort.class);
            com.majortom.algorithms.structure.array.Array<Integer> array =
                    new com.majortom.algorithms.structure.array.Array<>(request.values());
            return new PreparedExecution(() -> {
                algorithm.sort(array);
                java.util.ArrayList<Integer> values = new java.util.ArrayList<>(array.size());
                for (Integer value : array) {
                    values.add(value);
                }
                return List.copyOf(values);
            });
        }
    }

    private static final class ArrayMazeGeneratorHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(Boolean.class)
                    && ArrayMazeGenerator.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return MazeGenerationRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return GridMaze.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            MazeGenerationRequest request = mapper.convertValue(input, MazeGenerationRequest.class);
            ArrayMazeGenerator algorithm = createAlgorithm(descriptor, ArrayMazeGenerator.class);
            return new PreparedExecution(() -> algorithm.generate(request.dimensions(), request.seed()));
        }
    }

    private static final class GraphMazeGeneratorHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(Integer.class)
                    && GraphMazeGenerator.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return MazeGenerationRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return GraphSnapshot.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            MazeGenerationRequest request = mapper.convertValue(input, MazeGenerationRequest.class);
            @SuppressWarnings("unchecked")
            GraphMazeGenerator<Integer> algorithm =
                    (GraphMazeGenerator<Integer>) createAlgorithm(descriptor, GraphMazeGenerator.class);
            return new PreparedExecution(() -> algorithm.generate(request.dimensions(), request.seed()));
        }
    }

    private static final class ArrayMazePathfinderHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(Boolean.class)
                    && ArrayMazePathfinder.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return MazePathRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return List.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            MazePathRequest request = mapper.convertValue(input, MazePathRequest.class);
            ArrayMazePathfinder algorithm = createAlgorithm(descriptor, ArrayMazePathfinder.class);
            return new PreparedExecution(() ->
                    algorithm.findPath(request.maze(), request.start(), request.goal()));
        }
    }

    private static final class GraphTraversalHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(Integer.class)
                    && GraphTraversal.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return GraphBfsRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return List.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            GraphBfsRequest request = mapper.convertValue(input, GraphBfsRequest.class);
            Graph<Integer> graph = Graph.fromSnapshot(request.graph());
            @SuppressWarnings("unchecked")
            GraphTraversal<Integer> algorithm =
                    (GraphTraversal<Integer>) createAlgorithm(descriptor, GraphTraversal.class);
            return new PreparedExecution(() -> algorithm.traverse(graph, request.startNode()));
        }
    }

    private static final class StringSearchHandler implements ApiExecutionHandler {
        @Override
        public boolean supports(AlgorithmDescriptor descriptor) {
            return descriptor.valueType().equals(String.class)
                    && StringSearch.class.isAssignableFrom(descriptor.implementation());
        }

        @Override
        public Class<?> inputType() {
            return StringSearchRequest.class;
        }

        @Override
        public Class<?> outputType() {
            return List.class;
        }

        @Override
        public PreparedExecution prepare(
                AlgorithmDescriptor descriptor, Map<String, Object> input, ObjectMapper mapper) {
            StringSearchRequest request = mapper.convertValue(input, StringSearchRequest.class);
            StringStructure target = new com.majortom.algorithms.structure.string.String(request.target());
            StringSearch algorithm = createAlgorithm(descriptor, StringSearch.class);
            return new PreparedExecution(() -> algorithm.search(target, request.pattern()));
        }
    }
}
