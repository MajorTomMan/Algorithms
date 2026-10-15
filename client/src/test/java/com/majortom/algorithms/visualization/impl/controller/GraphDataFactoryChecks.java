package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Headless checks for multi-edge random generation, input and UI snapshot queries. */
public final class GraphDataFactoryChecks {
    public static void main(String[] args) {
        var multi = GraphDataFactory.randomGraphBatch(Integer.class, 4, 17,
                GraphDirection.UNDIRECTED, new Random(17));
        GraphSnapshot<Object> snapshot = GraphDataFactory.snapshot(multi, GraphDirection.UNDIRECTED);
        check(multi.edges().size() == 17 && snapshot.edges().size() == 17,
                "requested parallel edge count");
        Set<String> pairs = new HashSet<>();
        boolean parallel = false, loop = false;
        for (var edge : multi.edges()) {
            int a = (Integer) edge.from(), b = (Integer) edge.to();
            if (a == b) loop = true;
            String pair = Math.min(a, b) + ":" + Math.max(a, b);
            if (!pairs.add(pair)) parallel = true;
        }
        check(parallel && loop, "default random graph showcases both special edge types");

        var simple = GraphDataFactory.randomGraphBatch(Integer.class, 4, 6,
                GraphDirection.UNDIRECTED, new Random(17), false, false);
        Set<String> unique = new HashSet<>();
        for (var edge : simple.edges()) {
            int a = (Integer) edge.from(), b = (Integer) edge.to();
            check(a != b && unique.add(Math.min(a, b) + ":" + Math.max(a, b)),
                    "simple generation respects uniqueness");
        }
        check(simple.edges().size() == 6, "simple graph fills full capacity");
        rejects(() -> GraphDataFactory.randomGraphBatch(Integer.class, 2, 2,
                GraphDirection.UNDIRECTED, new Random(1), false, false),
                "impossible simple graph must fail fast");
        rejects(() -> GraphDataFactory.randomGraphBatch(Integer.class, 1, 1,
                GraphDirection.DIRECTED, new Random(1), true, false),
                "one vertex without loops cannot have edges");
        rejects(() -> GraphDataFactory.randomGraphBatch(Integer.class, 0, 1,
                GraphDirection.DIRECTED, new Random(1)), "empty graph with edges");
        check(GraphDataFactory.randomGraphBatch(Integer.class, 4, 0,
                GraphDirection.DIRECTED, new Random(1)).edges().isEmpty(),
                "zero requested edges must remain zero");

        var parsed = GraphBatchParser.parse("1,2|1>2:3 1>2:5 2>2:7",
                ValueAdapters.requireObjectAdapter(Integer.class), Integer.class,
                input -> java.util.Arrays.stream(input.split(","))
                        .map(String::trim).map(Integer::parseInt).map(x -> (Object) x).toList(),
                (key, params) -> { throw new AssertionError("Unexpected parsing error: " + key); });
        check(parsed != null && parsed.edges().size() == 3,
                "text batch supports parallel edges and self-loops");
        GraphSnapshot<Object> parsedSnapshot =
                GraphDataFactory.snapshot(parsed, GraphDirection.DIRECTED);
        check(parsedSnapshot.edges().size() == 3, "text batch preserves edge identities");
        long node = parsedSnapshot.vertices().get(1).id();
        check(GraphSnapshotQueries.snapshotDegree(parsedSnapshot, node) == 4,
                "loop contributes twice to degree plus two parallel edges");

        System.out.println("GraphDataFactoryChecks passed");
    }

    private static void rejects(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, message);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
