package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.algorithm.array.sort.insertion.IntegerInsertionSort;
import com.majortom.algorithms.algorithm.array.sort.insertion.StringInsertionSort;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.timeline.Timeline;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.visualization.runtime.array.ArrayEventReducer;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicValueReplayTest {
    @Test
    void integerAndStringArraysUseTheSameReducerAndReplayFactualEvents() {
        List<Integer> integerInput = List.of(4, 1, 3, 2);
        Array<Integer> integers = new Array<>(integerInput);
        Timeline integerTimeline = new Timeline();
        new ExecutionRuntime().execute("integer-insertion", integerTimeline,
                () -> { new IntegerInsertionSort().sort(integers); return null; });
        ArrayViewState integerState = replay(new ArrayEventReducer(integerInput), integerTimeline);
        assertEquals(List.of(1, 2, 3, 4), integerState.values().stream().map(VisualValue::value).toList());
        assertTrue(integerState.completed());

        SequenceSnapshot<String> snapshot = new SequenceSnapshot<>(List.of("delta", "alpha", "charlie", "bravo"));
        Array<String> strings = new Array<>(snapshot.values());
        Timeline stringTimeline = new Timeline();
        new ExecutionRuntime().execute("string-insertion", stringTimeline,
                () -> { new StringInsertionSort().sort(strings); return null; });
        ArrayViewState stringState = replay(new ArrayEventReducer(snapshot.values()), stringTimeline);
        assertEquals(List.of("alpha", "bravo", "charlie", "delta"),
                stringState.values().stream().map(VisualValue::value).toList());
        assertTrue(stringState.completed());
        assertFalse(integerTimeline.isEmpty());
        assertFalse(stringTimeline.isEmpty());
    }

    @Test
    void linkedTreeAndGraphProjectionKeepTopologyWhileAcceptingStringValues() {
        LinkedListViewState linked = LinkedListViewState.fromValues(List.of("left", "right"));
        assertEquals("left", linked.nodes().get(1L).value().text());
        assertEquals(2L, linked.nodes().get(1L).nextId());

        GeneralTreeSnapshot.Node<String> child = new GeneralTreeSnapshot.Node<>(2L, "child", List.of());
        GeneralTreeSnapshot<String> treeSnapshot = new GeneralTreeSnapshot<>(
                new GeneralTreeSnapshot.Node<>(1L, "root", List.of(child)), 2);
        TreeViewState tree = TreeViewState.general(treeSnapshot);
        assertEquals("root", tree.nodes().get(1L).value().text());
        assertEquals(List.of(2L), tree.nodes().get(1L).childIds());

        GraphSnapshot<String> graphSnapshot = new GraphSnapshot<>(false,
                List.of(new GraphSnapshot.Vertex<>(1L, "A"), new GraphSnapshot.Vertex<>(2L, "B")),
                List.of(new GraphSnapshot.Edge(1L, 1L, 2L)));
        GraphViewState graph = GraphViewState.initial(graphSnapshot);
        assertEquals("A", graph.nodesById().get(1L).value().text());
        assertEquals(1L, graph.edges().getFirst().fromId());
        assertEquals(2L, graph.edges().getFirst().toId());
    }

    private static ArrayViewState replay(ArrayEventReducer reducer, Timeline timeline) {
        ArrayViewState state = reducer.initialState();
        for (var envelope : timeline.events()) {
            state = reducer.reduce(state, envelope).state();
        }
        return state;
    }
}
