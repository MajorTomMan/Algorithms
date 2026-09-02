package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.library.graph.GraphBfs;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;
import com.majortom.algorithms.library.structure.MutableGraph;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.tree.AvlCommand;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.maze.MazeEventReducer;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeEventReducer;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeViewState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionProjectionTest {

    @Test
    void projectsAvlProviderEvents() {
        InMemoryEventSink sink = run("tree-avl", AvlTreeInput.fromValues(
                List.of(3, 1, 5), List.of(new AvlCommand(AvlCommand.Operation.INSERT, 4))));
        AvlTreeViewState state = replay(sink, new AvlTreeEventReducer());
        assertEquals(List.of(1, 3, 4, 5), state.values());
        assertTrue(state.completed());
    }

    @Test
    void projectsArrayMazeProviderEvents() {
        InMemoryEventSink sink = run(
                "maze-generator-dfs", new ArrayMazeGenerationInput(11, 11, 7L));
        MazeViewState state = replay(sink, new MazeEventReducer(11, 11, false));
        assertTrue(state.openCells().stream().anyMatch(Boolean::booleanValue));
        assertTrue(state.completed());
    }

    @Test
    void projectsGraphBfsProviderEvents() {
        MutableGraph<Integer> graph = new MutableGraph<>();
        graph.addVertex(0);
        graph.addVertex(1);
        graph.addVertex(2);
        graph.addEdge(0, 1);
        graph.addEdge(1, 2);
        IntGraph snapshot = GraphBfs.snapshot(graph);
        ModuleRegistry registry = ModuleLoader.load();
        InMemoryEventSink sink = new InMemoryEventSink();
        GraphBfs algorithm = registry.create("algorithm.graph.Integer.graph-bfs", GraphBfs.class);
        new ExecutionRuntime().execute("graph-bfs", sink, () -> algorithm.traverse(graph, 0));
        GraphViewState state = replay(sink, new GraphEventReducer(snapshot));
        assertEquals(List.of(0, 1, 2), state.visited());
        assertTrue(state.completed());
    }

    private InMemoryEventSink run(String id, Object input) {
        ModuleRegistry registry = ModuleLoader.load();
        InMemoryEventSink sink = new InMemoryEventSink();
        if (id.equals("tree-avl")) {
            AvlTreeCommands algorithm = registry.create("algorithm.tree.Integer.tree-avl", AvlTreeCommands.class);
            new ExecutionRuntime().execute(id, sink, () -> algorithm.execute((AvlTreeInput) input));
        } else if (id.equals("maze-generator-dfs")) {
            ArrayMazeGenerator algorithm = registry.create("algorithm.maze.Boolean.maze-generator-dfs", ArrayMazeGenerator.class);
            new ExecutionRuntime().execute(id, sink, () -> algorithm.generate((ArrayMazeGenerationInput) input));
        } else {
            throw new IllegalArgumentException("Unknown projection operation: " + id);
        }
        return sink;
    }

    private <S> S replay(InMemoryEventSink sink, EventReducer<S> reducer) {
        ReducedEventTimeline<S> timeline = new ReducedEventTimeline<>(sink.events(), reducer);
        return timeline.seek(timeline.size() - 1);
    }
}
