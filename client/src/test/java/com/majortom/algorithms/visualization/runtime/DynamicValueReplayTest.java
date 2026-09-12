package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.algorithm.array.sort.insertion.IntegerInsertionSort;
import com.majortom.algorithms.algorithm.array.sort.insertion.StringInsertionSort;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.SequenceSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.snapshot.WeightedGraphSnapshot;
import com.majortom.algorithms.core.timeline.Timeline;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.visualization.runtime.array.ArrayEventReducer;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphEventReducer;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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


    @Test
    void structureSnapshotCarriesValueTypeAndRejectsMismatches() {
        StructureSnapshot<SequenceSnapshot<String>> snapshot = StructureSnapshot.create(
                "array", String.class, new SequenceSnapshot<>(List.of("alpha", "beta")));

        assertTrue(snapshot.matchesValueType(String.class));
        assertDoesNotThrow(() -> snapshot.requireValueType(String.class));
        IllegalArgumentException mismatch = assertThrows(
                IllegalArgumentException.class, () -> snapshot.requireValueType(Integer.class));
        assertTrue(mismatch.getMessage().contains("value type mismatch"));
    }

    @Test
    void customCityGraphAndWeightedGraphReuseTheSameGraphProjectionAndReducer() {
        City tokyo = new City("Tokyo", 35.6762d, 139.6503d);
        City yokohama = new City("Yokohama", 35.4437d, 139.6380d);
        City chiba = new City("Chiba", 35.6074d, 140.1065d);

        Graph<City> graph = new Graph<>(false);
        LinkedHashMap<City, java.util.Collection<City>> adjacency = new LinkedHashMap<>();
        adjacency.put(tokyo, List.of(yokohama));
        adjacency.put(yokohama, List.of(tokyo));
        graph.initialize(adjacency);

        GraphSnapshot<City> initial = snapshot(graph);
        Timeline timeline = new Timeline();
        new ExecutionRuntime().execute("city-graph-mutation", timeline, () -> {
            graph.addVertex(chiba);
            graph.addEdge(graph.vertex(tokyo), graph.vertex(chiba));
            return null;
        });

        GraphViewState replayed = replay(new GraphEventReducer(initial), timeline);
        assertEquals(3, replayed.nodes().size());
        assertTrue(replayed.nodes().stream().anyMatch(node -> node.value().value().equals(chiba)));
        assertEquals(chiba.toString(), replayed.nodes().stream()
                .filter(node -> node.value().value().equals(chiba))
                .findFirst().orElseThrow().value().text());

        WeightedGraph<City> weighted = new WeightedGraph<>(false);
        LinkedHashMap<City, Map<City, Double>> weightedAdjacency = new LinkedHashMap<>();
        weightedAdjacency.put(tokyo, Map.of(yokohama, 29.5d));
        weightedAdjacency.put(yokohama, Map.of(tokyo, 29.5d));
        weighted.initializeWeighted(weightedAdjacency);
        WeightedGraphSnapshot<City> weightedSnapshot = weighted.snapshot();
        GraphViewState weightedState = GraphViewState.initial(weightedSnapshot);

        assertEquals(2, weightedState.nodes().size());
        assertEquals(29.5d, weightedState.edges().getFirst().weight());
        assertTrue(weightedState.nodes().stream().allMatch(node -> node.value().value() instanceof City));
    }


    private static <T> GraphSnapshot<T> snapshot(Graph<T> graph) {
        java.util.ArrayList<GraphSnapshot.Vertex<T>> vertices = new java.util.ArrayList<>();
        for (var vertex : graph.vertices()) {
            vertices.add(new GraphSnapshot.Vertex<>(vertex.id(), vertex.value()));
        }
        java.util.ArrayList<GraphSnapshot.Edge> edges = new java.util.ArrayList<>();
        for (var edge : graph.edges()) {
            edges.add(new GraphSnapshot.Edge(edge.id(), edge.from().id(), edge.to().id()));
        }
        return new GraphSnapshot<>(graph.isDirected(), vertices, edges);
    }

    private static ArrayViewState replay(ArrayEventReducer reducer, Timeline timeline) {
        ArrayViewState state = reducer.initialState();
        for (var envelope : timeline.events()) {
            state = reducer.reduce(state, envelope).state();
        }
        return state;
    }

    private static GraphViewState replay(GraphEventReducer reducer, Timeline timeline) {
        GraphViewState state = reducer.initialState();
        for (var envelope : timeline.events()) {
            state = reducer.reduce(state, envelope).state();
        }
        return state;
    }

    private record City(String name, double latitude, double longitude) {
        @Override
        public String toString() {
            return name;
        }
    }
}
