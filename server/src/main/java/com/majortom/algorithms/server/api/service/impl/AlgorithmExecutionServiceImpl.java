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
import com.majortom.algorithms.library.graph.GraphBfsOutput;
import com.majortom.algorithms.library.basic.graph.Graph;
import com.majortom.algorithms.library.structure.StringStructure;
import com.majortom.algorithms.library.string.KmpSearch;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationOutput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathInput;
import com.majortom.algorithms.library.maze.ArrayMazePathOutput;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeBfsGenerator;
import com.majortom.algorithms.library.maze.GraphMazeGenerationInput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationOutput;
import com.majortom.algorithms.library.sort.AbstractIntegerSort;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.library.tree.AvlTreeOutput;
import com.majortom.algorithms.server.api.constant.ExecutionState;
import com.majortom.algorithms.server.api.entity.ExecutionUnit;
import com.majortom.algorithms.server.api.service.AlgorithmExecutionService;
import com.majortom.algorithms.server.dto.AlgorithmInformationDto;
import com.majortom.algorithms.server.request.ExecutionRequest;
import com.majortom.algorithms.server.request.GraphBfsRequest;
import com.majortom.algorithms.server.request.StringSearchRequest;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class AlgorithmExecutionServiceImpl implements AlgorithmExecutionService {

    private static final String VERSION = "2.0";
    private static final ModuleRegistry MODULES = ModuleLoader.load();
    private static final List<AlgorithmApiDescriptor> ALGORITHMS = List.of(
            new AlgorithmApiDescriptor("insertion-sort", "sort", IntegerSortInput.class, IntegerSortOutput.class),
            new AlgorithmApiDescriptor("selection-sort", "sort", IntegerSortInput.class, IntegerSortOutput.class),
            new AlgorithmApiDescriptor("quick-sort", "sort", IntegerSortInput.class, IntegerSortOutput.class),
            new AlgorithmApiDescriptor("heap-sort", "sort", IntegerSortInput.class, IntegerSortOutput.class),
            new AlgorithmApiDescriptor("maze-generator-bfs", "maze", ArrayMazeGenerationInput.class, ArrayMazeGenerationOutput.class),
            new AlgorithmApiDescriptor("maze-generator-dfs", "maze", ArrayMazeGenerationInput.class, ArrayMazeGenerationOutput.class),
            new AlgorithmApiDescriptor("maze-generator-union-find", "maze", ArrayMazeGenerationInput.class, ArrayMazeGenerationOutput.class),
            new AlgorithmApiDescriptor("graph-generator-bfs", "maze", GraphMazeGenerationInput.class, GraphMazeGenerationOutput.class),
            new AlgorithmApiDescriptor("maze-pathfinder-astar", "maze", ArrayMazePathInput.class, ArrayMazePathOutput.class),
            new AlgorithmApiDescriptor("maze-pathfinder-dfs", "maze", ArrayMazePathInput.class, ArrayMazePathOutput.class),
            new AlgorithmApiDescriptor("tree-avl", "tree", AvlTreeInput.class, AvlTreeOutput.class),
            new AlgorithmApiDescriptor("graph-bfs", "graph", GraphBfsRequest.class, GraphBfsOutput.class),
            new AlgorithmApiDescriptor("kmp", "string", StringSearchRequest.class, List.class));

    private final ObjectMapper objectMapper;
    private final ExecutionScheduler executionScheduler = ExecutionScheduler.bounded("algorithm-executor-", 10, 20, 100);
    private final Map<String, ExecutionUnit> executions = new ConcurrentHashMap<>();

    public AlgorithmExecutionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    void closeScheduler() {
        executionScheduler.close();
    }

    @Override
    public String execute(ExecutionRequest request) {
        PreparedExecution prepared = prepareExecution(request.getAlgorithmId(), request.getInput());
        String runId = UUID.randomUUID().toString();
        ExecutionUnit unit = new ExecutionUnit();
        unit.setRunId(runId);
        unit.setAlgorithmId(request.getAlgorithmId());
        unit.setStatus(ExecutionState.QUEUED);
        executions.put(runId, unit);
        executionScheduler.execute(() -> runExecution(unit, prepared.operation()));
        return runId;
    }

    private void runExecution(ExecutionUnit unit, ExecutionOperation<?> operation) {
        unit.setStatus(ExecutionState.RUNNING);
        log.info("Starting execution for runId: {}", unit.getRunId());
        RecordingEventSink eventSink = new RecordingEventSink();
        ExecutionRuntime runtime = new ExecutionRuntime(Clock.systemUTC(), unit::getRunId);
        ExecutionResult result = runtime.execute(unit.getAlgorithmId(), eventSink, operation);
        ExecutionRecording recording = eventSink.snapshot();
        unit.setDuration(recording.statistics().duration().toMillis());
        unit.setTotalEventCount(recording.statistics().totalEventCount());
        switch (result.status()) {
            case COMPLETED -> unit.setStatus(ExecutionState.COMPLETED);
            case CANCELLED -> unit.setStatus(ExecutionState.CANCELLED);
            case FAILED -> unit.setStatus(ExecutionState.FAILED);
        }
    }

    @Override
    public List<AlgorithmInformationDto> getAlgorithms() {
        return ALGORITHMS.stream().map(descriptor -> {
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
            throw new IllegalArgumentException("No execution found for runId: " + runId);
        }
        return unit;
    }

    private PreparedExecution prepareExecution(String algorithmId, Map<String, Object> rawInput) {
        return switch (algorithmId) {
            case "insertion-sort", "selection-sort", "quick-sort", "heap-sort" -> {
                IntegerSortInput input = objectMapper.convertValue(rawInput, IntegerSortInput.class);
                AbstractIntegerSort algorithm = MODULES.create("algorithm.array.Integer." + algorithmId, AbstractIntegerSort.class);
                yield new PreparedExecution(() -> algorithm.sort(input));
            }
            case "maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find" -> {
                ArrayMazeGenerationInput input = objectMapper.convertValue(rawInput, ArrayMazeGenerationInput.class);
                ArrayMazeGenerator algorithm = MODULES.create("algorithm.maze.Boolean." + algorithmId, ArrayMazeGenerator.class);
                yield new PreparedExecution(() -> algorithm.generate(input));
            }
            case "graph-generator-bfs" -> {
                GraphMazeGenerationInput input = objectMapper.convertValue(rawInput, GraphMazeGenerationInput.class);
                GraphMazeBfsGenerator algorithm = MODULES.create("algorithm.graph.Integer.graph-generator-bfs", GraphMazeBfsGenerator.class);
                yield new PreparedExecution(() -> algorithm.generate(input));
            }
            case "maze-pathfinder-astar", "maze-pathfinder-dfs" -> {
                ArrayMazePathInput input = objectMapper.convertValue(rawInput, ArrayMazePathInput.class);
                ArrayMazePathfinder algorithm = MODULES.create("algorithm.maze.Boolean." + algorithmId, ArrayMazePathfinder.class);
                yield new PreparedExecution(() -> algorithm.findPath(input));
            }
            case "tree-avl" -> {
                AvlTreeInput input = objectMapper.convertValue(rawInput, AvlTreeInput.class);
                AvlTreeCommands algorithm = MODULES.create("algorithm.tree.Integer.tree-avl", AvlTreeCommands.class);
                yield new PreparedExecution(() -> algorithm.execute(input));
            }
            case "graph-bfs" -> {
                GraphBfsRequest input = objectMapper.convertValue(rawInput, GraphBfsRequest.class);
                Graph<Integer> graph = Graph.fromSnapshot(input.graph());
                GraphBfs algorithm = MODULES.create("algorithm.graph.Integer.graph-bfs", GraphBfs.class);
                yield new PreparedExecution(() -> algorithm.traverse(graph, input.startNode()));
            }
            case "kmp" -> {
                StringSearchRequest input = objectMapper.convertValue(rawInput, StringSearchRequest.class);
                StringStructure target = new com.majortom.algorithms.library.basic.String(input.target());
                KmpSearch algorithm = MODULES.create("algorithm.string.String.kmp", KmpSearch.class);
                yield new PreparedExecution(() -> algorithm.search(target, input.pattern()));
            }
            default -> throw new IllegalArgumentException("Unknown algorithm ID: " + algorithmId);
        };
    }

    private record PreparedExecution(ExecutionOperation<?> operation) {
    }

    private record AlgorithmApiDescriptor(String id, String moduleId, Class<?> inputType, Class<?> outputType) {
    }
}
