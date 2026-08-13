package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.library.graph.GraphBfsInput;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.tree.AvlCommand;
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
        InMemoryEventSink sink = run("tree-avl", new AvlTreeInput(
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
        IntGraph graph = new IntGraph(
                List.of(0, 1, 2), List.of(new IntEdge(0, 1), new IntEdge(1, 2)));
        InMemoryEventSink sink = run("graph-bfs", new GraphBfsInput(graph, 0));
        GraphViewState state = replay(sink, new GraphEventReducer(graph));
        assertEquals(List.of(0, 1, 2), state.visited());
        assertTrue(state.completed());
    }

    private InMemoryEventSink run(String id, com.majortom.algorithms.core.api.AlgorithmInput input) {
        InMemoryEventSink sink = new InMemoryEventSink();
        new DefaultAlgorithmRunner().run(ProviderCatalog.production().require(id).invoker(), input, sink);
        return sink;
    }

    private <S> S replay(InMemoryEventSink sink, EventReducer<S> reducer) {
        ReducedEventTimeline<S> timeline = new ReducedEventTimeline<>(sink.events(), reducer);
        return timeline.seek(timeline.size() - 1);
    }
}
