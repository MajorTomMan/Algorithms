package com.majortom.algorithms.server.api.service.impl;

import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.core.runtime.ExecutionOperation;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionScheduler;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.library.graph.GraphBfs;
import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.structure.StringStructure;
import com.majortom.algorithms.library.string.KmpSearch;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeBfsGenerator;
import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.library.maze.GridPoint;
import com.majortom.algorithms.library.maze.MazeDimensions;
import com.majortom.algorithms.library.sort.Sort;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.basic.tree.AVLTree;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.server.api.constant.ExecutionState;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.error.AlgorithmNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionNotFoundException;
import com.majortom.algorithms.server.api.error.ExecutionRejectedException;
import com.majortom.algorithms.server.api.service.AlgorithmExecutionService;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import com.majortom.algorithms.server.request.IntegerSortRequest;
import com.majortom.algorithms.server.request.MazeGenerationRequest;
import com.majortom.algorithms.server.request.MazePathRequest;
import com.majortom.algorithms.server.request.AvlTreeRequest;
import com.majortom.algorithms.server.request.GraphBfsRequest;
import com.majortom.algorithms.server.request.StringSearchRequest;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Service
@Log4j2
public class AlgorithmExecutionServiceImpl implements AlgorithmExecutionService {

    private static final String VERSION = "2.0";
    private static final int MAX_RETAINED_EXECUTIONS = 512;
    private static final ModuleRegistry MODULES = ModuleLoader.load();
    private static final List<AlgorithmApiDescriptor> ALGORITHMS = List.of(
            new AlgorithmApiDescriptor("insertion-sort", "array", "algorithm.array.Integer.insertion-sort", IntegerSortRequest.class, List.class),
            new AlgorithmApiDescriptor("selection-sort", "array", "algorithm.array.Integer.selection-sort", IntegerSortRequest.class, List.class),
            new AlgorithmApiDescriptor("quick-sort", "array", "algorithm.array.Integer.quick-sort", IntegerSortRequest.class, List.class),
            new AlgorithmApiDescriptor("heap-sort", "array", "algorithm.array.Integer.heap-sort", IntegerSortRequest.class, List.class),
            new AlgorithmApiDescriptor("maze-generator-bfs", "maze", "algorithm.maze.Boolean.maze-generator-bfs", MazeGenerationRequest.class, GridMaze.class),
            new AlgorithmApiDescriptor("maze-generator-dfs", "maze", "algorithm.maze.Boolean.maze-generator-dfs", MazeGenerationRequest.class, GridMaze.class),
            new AlgorithmApiDescriptor("maze-generator-union-find", "maze", "algorithm.maze.Boolean.maze-generator-union-find", MazeGenerationRequest.class, GridMaze.class),
            new AlgorithmApiDescriptor("graph-generator-bfs", "maze", "algorithm.graph.Integer.graph-generator-bfs", MazeGenerationRequest.class, GraphSnapshot.class),
            new AlgorithmApiDescriptor("maze-pathfinder-astar", "maze", "algorithm.maze.Boolean.maze-pathfinder-astar", MazePathRequest.class, List.class),
            new AlgorithmApiDescriptor("maze-pathfinder-dfs", "maze", "algorithm.maze.Boolean.maze-pathfinder-dfs", MazePathRequest.class, List.class),
            new AlgorithmApiDescriptor("tree-avl", "tree", "algorithm.tree.Integer.tree-avl", AvlTreeRequest.class, AvlNodeSnapshot.class),
            new AlgorithmApiDescriptor("graph-bfs", "graph", "algorithm.graph.Integer.graph-bfs", GraphBfsRequest.class, List.class),
            new AlgorithmApiDescriptor("kmp", "string", "algorithm.string.String.kmp", StringSearchRequest.class, List.class));

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
        return ALGORITHMS.stream()
                .filter(descriptor -> MODULES.contains(descriptor.registryKey()))
                .map(descriptor -> {
            AlgorithmInformationDto dto = new AlgorithmInformationDto();
            dto.setId(descriptor.id());
            dto.setModuleId(descriptor.moduleId());
            dto.setVersion(VERSION);
            dto.setInputType(descriptor.inputType().getName());
            dto.setOutputType(descriptor.outputType().getName());
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
        AlgorithmApiDescriptor descriptor = descriptor(algorithmId);
        if (!MODULES.contains(descriptor.registryKey())) {
            throw new AlgorithmNotFoundException(algorithmId);
        }
        Map<String, Object> input = rawInput == null ? Map.of() : rawInput;
        return switch (algorithmId) {
            case "insertion-sort", "selection-sort", "quick-sort", "heap-sort" -> {
                IntegerSortRequest request = objectMapper.convertValue(input, IntegerSortRequest.class);
                @SuppressWarnings("unchecked")
                Sort<Integer> algorithm = (Sort<Integer>) MODULES.create(
                        "algorithm.array.Integer." + algorithmId, Sort.class);
                com.majortom.algorithms.library.basic.Array<Integer> array =
                        new com.majortom.algorithms.library.basic.Array<>(request.values());
                yield new PreparedExecution(() -> {
                    algorithm.sort(array);
                    return copy(array);
                });
            }
            case "maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find" -> {
                MazeGenerationRequest request = objectMapper.convertValue(input, MazeGenerationRequest.class);
                MazeDimensions dimensions = request.dimensions();
                ArrayMazeGenerator algorithm = MODULES.create(
                        "algorithm.maze.Boolean." + algorithmId, ArrayMazeGenerator.class);
                yield new PreparedExecution(() -> algorithm.generate(dimensions, request.seed()));
            }
            case "graph-generator-bfs" -> {
                MazeGenerationRequest request = objectMapper.convertValue(input, MazeGenerationRequest.class);
                MazeDimensions dimensions = request.dimensions();
                GraphMazeBfsGenerator algorithm = MODULES.create(
                        "algorithm.graph.Integer.graph-generator-bfs", GraphMazeBfsGenerator.class);
                yield new PreparedExecution(() -> algorithm.generate(dimensions, request.seed()));
            }
            case "maze-pathfinder-astar", "maze-pathfinder-dfs" -> {
                MazePathRequest request = objectMapper.convertValue(input, MazePathRequest.class);
                ArrayMazePathfinder algorithm = MODULES.create(
                        "algorithm.maze.Boolean." + algorithmId, ArrayMazePathfinder.class);
                yield new PreparedExecution(() -> algorithm.findPath(request.maze(), request.start(), request.goal()));
            }
            case "tree-avl" -> {
                AvlTreeRequest request = objectMapper.convertValue(input, AvlTreeRequest.class);
                AvlTreeCommands algorithm = MODULES.create(
                        "algorithm.tree.Integer.tree-avl", AvlTreeCommands.class);
                yield new PreparedExecution(() -> {
                    AVLTree<Integer> tree = request.toTree();
                    algorithm.execute(tree, request.commands());
                    return snapshot(tree.root());
                });
            }
            case "graph-bfs" -> {
                GraphBfsRequest request = objectMapper.convertValue(input, GraphBfsRequest.class);
                Graph<Integer> graph = Graph.fromSnapshot(request.graph());
                GraphBfs algorithm = MODULES.create("algorithm.graph.Integer.graph-bfs", GraphBfs.class);
                yield new PreparedExecution(() -> algorithm.traverse(graph, request.startNode()));
            }
            case "kmp" -> {
                StringSearchRequest request = objectMapper.convertValue(input, StringSearchRequest.class);
                StringStructure target = new com.majortom.algorithms.library.basic.String(request.target());
                KmpSearch algorithm = MODULES.create("algorithm.string.String.kmp", KmpSearch.class);
                yield new PreparedExecution(() -> algorithm.search(target, request.pattern()));
            }
            default -> throw new AlgorithmNotFoundException(algorithmId);
        };
    }

    private AlgorithmApiDescriptor descriptor(String algorithmId) {
        if (algorithmId == null || algorithmId.isBlank()) {
            throw new IllegalArgumentException("algorithmId must not be blank");
        }
        return ALGORITHMS.stream()
                .filter(candidate -> candidate.id().equals(algorithmId))
                .findFirst()
                .orElseThrow(() -> new AlgorithmNotFoundException(algorithmId));
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
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private List<Integer> copy(com.majortom.algorithms.library.structure.ArrayStructure<Integer> array) {
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>(array.size());
        for (Integer value : array) {
            values.add(value);
        }
        return List.copyOf(values);
    }

    private AvlNodeSnapshot snapshot(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new AvlNodeSnapshot(node.getId(), node.getValue(), node.getHeight(),
                snapshot(left(node)), snapshot(right(node)));
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> left(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.getLeft();
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> right(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.getRight();
    }

    private record PreparedExecution(ExecutionOperation<?> operation) {
    }

    private record AlgorithmApiDescriptor(
            String id, String moduleId, String registryKey, Class<?> inputType, Class<?> outputType) {
    }
}
