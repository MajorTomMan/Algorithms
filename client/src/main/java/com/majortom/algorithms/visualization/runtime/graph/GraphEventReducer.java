package com.majortom.algorithms.visualization.runtime.graph;

import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.library.graph.GraphBfsEvent;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stateless reducer for graph traversal events. */
public final class GraphEventReducer implements EventReducer<GraphViewState> {

    private final IntGraph initialGraph;

    public GraphEventReducer(IntGraph graph) {
        initialGraph = graph;
    }

    @Override
    public GraphViewState initialState() {
        return GraphViewState.initial(initialGraph);
    }

    @Override
    public Reduction<GraphViewState> reduce(GraphViewState previous, EventEnvelope event) {
        Object payload = event.event();
        if (payload instanceof GraphBfsEvent.Initialized initialized) {
            GraphViewState state = new GraphViewState(initialized.graph(),
                    Set.of(initialized.startNode()), Set.of(), List.of(), Map.of(), initialized.startNode(),
                    null, GraphViewState.Phase.INITIALIZED, false);
            return changed(state, EventImportance.CHECKPOINT);
        }
        if (payload instanceof GraphBfsEvent.Discovered discovered) {
            Set<Integer> nodes = new LinkedHashSet<>(previous.discovered());
            nodes.add(discovered.node());
            Map<Integer, Integer> parents = new LinkedHashMap<>(previous.parents());
            if (discovered.parent() != null) {
                parents.putIfAbsent(discovered.node(), discovered.parent());
            }
            GraphViewState state = copy(previous, nodes, previous.entered(), previous.visited(), parents,
                    discovered.node(), edge(discovered.parent(), discovered.node()),
                    GraphViewState.Phase.DISCOVERING, false);
            return changed(state, EventImportance.TRANSIENT);
        }
        if (payload instanceof GraphBfsEvent.Entered entered) {
            Set<Integer> nodes = new LinkedHashSet<>(previous.entered());
            nodes.add(entered.node());
            Map<Integer, Integer> parents = new LinkedHashMap<>(previous.parents());
            if (entered.parent() != null) {
                parents.putIfAbsent(entered.node(), entered.parent());
            }
            GraphViewState state = copy(previous, previous.discovered(), nodes, previous.visited(), parents,
                    entered.node(), edge(entered.parent(), entered.node()), GraphViewState.Phase.ENTERING, false);
            return changed(state, EventImportance.TRANSIENT);
        }
        if (payload instanceof GraphBfsEvent.EdgeExamined examined) {
            GraphViewState state = copy(previous, previous.discovered(), previous.entered(), previous.visited(),
                    previous.parents(), examined.from(), new IntEdge(examined.from(), examined.to()),
                    GraphViewState.Phase.EXAMINING_EDGE, false);
            return changed(state, EventImportance.TRANSIENT);
        }
        if (payload instanceof GraphBfsEvent.Visited visited) {
            List<Integer> nodes = new ArrayList<>(previous.visited());
            if (!nodes.contains(visited.node())) {
                nodes.add(visited.node());
            }
            GraphViewState state = copy(previous, previous.discovered(), previous.entered(), nodes,
                    previous.parents(), visited.node(), previous.examinedEdge(),
                    GraphViewState.Phase.VISITING, false);
            return changed(state, EventImportance.STATE_CHANGE);
        }
        if (payload instanceof GraphBfsEvent.Completed completed) {
            GraphViewState state = copy(previous, previous.discovered(), previous.entered(),
                    completed.visitOrder(), previous.parents(), previous.focus(), null,
                    GraphViewState.Phase.COMPLETED, true);
            return changed(state, EventImportance.TERMINAL);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static GraphViewState copy(
            GraphViewState previous, Set<Integer> discovered, Set<Integer> entered,
            List<Integer> visited, Map<Integer, Integer> parents, Integer focus, IntEdge examinedEdge,
            GraphViewState.Phase phase, boolean completed) {
        return new GraphViewState(previous.graph(), discovered, entered, visited, parents, focus,
                examinedEdge, phase, completed);
    }

    private static IntEdge edge(Integer from, int to) {
        if (from == null) {
            return null;
        }
        return new IntEdge(from, to);
    }

    private static Reduction<GraphViewState> changed(GraphViewState state, EventImportance importance) {
        return Reduction.changed(state, importance, true);
    }
}
