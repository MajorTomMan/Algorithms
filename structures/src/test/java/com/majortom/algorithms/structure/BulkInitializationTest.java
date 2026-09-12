package com.majortom.algorithms.structure;

import com.majortom.algorithms.core.event.structure.StructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionRecording;
import com.majortom.algorithms.core.runtime.ExecutionRuntime;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.RecordingEventSink;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.structure.graph.Edge;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.Vertex;
import com.majortom.algorithms.structure.graph.WeightedGraph;
import com.majortom.algorithms.structure.linked.LinkedList;
import com.majortom.algorithms.structure.linked.ListNode;
import com.majortom.algorithms.structure.tree.AVLTree;
import com.majortom.algorithms.structure.tree.GeneralTreeStructure;
import com.majortom.algorithms.structure.tree.Tree;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class BulkInitializationTest {

    @Test
    void arrayAndLinkedBulkInitializationReplaceStateWithoutMutationEvents() {
        Array<String> array = new Array<>(List.of("old"));
        LinkedList<String> linked = new LinkedList<>();
        linked.initialize(List.of("old"));

        RecordingEventSink sink = new RecordingEventSink();
        var result = new ExecutionRuntime().execute("bulk-sequence", sink, () -> {
            array.initialize(List.of("alpha", "beta", "gamma"));
            linked.initialize(List.of("alpha", "beta", "gamma"));
            return null;
        });

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(List.of("alpha", "beta", "gamma"), values(array));
        assertEquals(List.of("alpha", "beta", "gamma"), values(linked));
        assertLinkedTopology(linked);
        assertNoStructureEvents(sink.snapshot());
    }

    @Test
    void treeBulkInitializationBuildsOrderedTopologyWithoutMutationEvents() {
        Tree<String> tree = new Tree<>();
        GeneralTreeStructure.NodeInput<String> input = new GeneralTreeStructure.NodeInput<>("root", List.of(
                GeneralTreeStructure.NodeInput.leaf("left"),
                new GeneralTreeStructure.NodeInput<>("right", List.of(
                        GeneralTreeStructure.NodeInput.leaf("leaf")))));

        RecordingEventSink sink = new RecordingEventSink();
        var result = new ExecutionRuntime().execute("bulk-tree", sink, () -> {
            tree.initialize(input);
            return null;
        });

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(4, tree.size());
        assertEquals("root", tree.root().getValue());
        assertEquals(List.of("left", "right"), tree.root().getChildren().stream()
                .map(node -> node.getValue()).toList());
        assertEquals("leaf", tree.root().getChildren().get(1).getChildren().getFirst().getValue());
        assertNoStructureEvents(sink.snapshot());
    }

    @Test
    void avlBulkInitializationBuildsBalancedTreeWithoutMutationEvents() {
        AVLTree<String> tree = new AVLTree<>();
        List<String> values = List.of("alpha", "beta", "delta", "epsilon", "gamma", "theta", "zeta");

        RecordingEventSink sink = new RecordingEventSink();
        var result = new ExecutionRuntime().execute("bulk-avl", sink, () -> {
            tree.initializeSorted(values);
            return null;
        });

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(values.size(), tree.size());
        assertEquals("epsilon", tree.root().getValue());
        assertEquals(3, tree.root().getHeight());
        assertEquals(values.size(), tree.root().getSubTreeCount());
        assertNoStructureEvents(sink.snapshot());
    }

    @Test
    void graphBulkInitializationBuildsTopologyWithoutMutationEvents() {
        Graph<String> graph = new Graph<>(false);
        Map<String, Collection<String>> adjacency = new LinkedHashMap<>();
        adjacency.put("A", List.of("B", "C"));
        adjacency.put("B", List.of("A", "C"));
        adjacency.put("C", List.of("A", "B", "D"));

        RecordingEventSink sink = new RecordingEventSink();
        var result = new ExecutionRuntime().execute("bulk-graph", sink, () -> {
            graph.initialize(adjacency);
            return null;
        });

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(4, graph.vertexCount());
        assertEquals(4, graph.edgeCount());
        assertEquals(List.of("B", "C"), vertexValues(graph.neighbors(graph.vertex("A"))));
        assertEquals(List.of("A", "B", "D"), vertexValues(graph.neighbors(graph.vertex("C"))));
        assertNoStructureEvents(sink.snapshot());
    }

    @Test
    void weightedGraphBulkInitializationPreservesWeightsWithoutMutationEvents() {
        WeightedGraph<String> graph = new WeightedGraph<>(false);
        Map<String, Map<String, Double>> adjacency = new LinkedHashMap<>();
        adjacency.put("Tokyo", linkedMap(Map.entry("Osaka", 2.5d), Map.entry("Nagoya", 1.25d)));
        adjacency.put("Osaka", linkedMap(Map.entry("Tokyo", 2.5d)));
        adjacency.put("Nagoya", linkedMap(Map.entry("Tokyo", 1.25d)));

        RecordingEventSink sink = new RecordingEventSink();
        var result = new ExecutionRuntime().execute("bulk-weighted-graph", sink, () -> {
            graph.initializeWeighted(adjacency);
            return null;
        });

        assertEquals(ExecutionStatus.COMPLETED, result.status());
        assertEquals(3, graph.vertexCount());
        assertEquals(2, graph.edgeCount());
        assertEquals(2.5d, graph.weight(graph.edge(graph.vertex("Tokyo"), graph.vertex("Osaka"))));
        assertEquals(1.25d, graph.weight(graph.edge(graph.vertex("Tokyo"), graph.vertex("Nagoya"))));
        assertNoStructureEvents(sink.snapshot());
    }

    @Test
    void largeGraphBulkInitializationStaysLinearEnoughForWorkbenchLoads() {
        int vertices = 20_000;
        LinkedHashMap<Integer, Collection<Integer>> adjacency = new LinkedHashMap<>(vertices);
        for (int value = 0; value < vertices; value++) {
            adjacency.put(value, List.of((value + 1) % vertices));
        }
        Graph<Integer> graph = new Graph<>(true);

        assertTimeout(Duration.ofSeconds(5), () -> graph.initialize(adjacency));
        assertEquals(vertices, graph.vertexCount());
        assertEquals(vertices, graph.edgeCount());
    }

    private static <T> List<T> values(Iterable<T> values) {
        ArrayList<T> copy = new ArrayList<>();
        values.forEach(copy::add);
        return List.copyOf(copy);
    }

    private static void assertLinkedTopology(LinkedList<String> linked) {
        ListNode<String> first = linked.head();
        ListNode<String> second = first.getNext();
        ListNode<String> third = second.getNext();
        assertNull(first.getPrevious());
        assertSame(first, second.getPrevious());
        assertSame(second, third.getPrevious());
        assertNull(third.getNext());
        assertSame(third, linked.tail());
    }

    private static <T> List<T> vertexValues(Iterable<Vertex<T>> vertices) {
        ArrayList<T> values = new ArrayList<>();
        vertices.forEach(vertex -> values.add(vertex.value()));
        return List.copyOf(values);
    }

    @SafeVarargs
    private static <K, V> Map<K, V> linkedMap(Map.Entry<K, V>... entries) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static void assertNoStructureEvents(ExecutionRecording recording) {
        assertFalse(recording.events().stream().anyMatch(event -> event.event() instanceof StructureEvent));
    }
}
