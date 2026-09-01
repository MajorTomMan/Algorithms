package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import javafx.application.Platform;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Incremental GraphStream adapter fed exclusively by the neutral graph projection. */
public final class GraphVisualizer extends BaseVisualizer<GraphViewState> {

    private final Graph graph = new SingleGraph("algorithm-graph");
    private FxViewer viewer;
    private FxViewPanel viewPanel;
    private boolean graphDisposed;
    private long accentUntilMillis;

    public GraphVisualizer() {
        System.setProperty("org.graphstream.ui", "javafx");
        graph.setStrict(false);
        Platform.runLater(this::initializeViewer);
    }

    private void initializeViewer() {
        if (graphDisposed || isDisposed()) {
            return;
        }
        graph.setAttribute("ui.antialias");
        java.net.URL css = getClass().getResource("/style/graph.css");
        if (css != null) {
            graph.setAttribute("ui.stylesheet", "url('" + css.toExternalForm() + "')");
        }
        viewer = new FxViewer(graph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        viewer.enableAutoLayout();
        viewPanel = (FxViewPanel) viewer.addDefaultView(false);
        viewPanel.prefWidthProperty().bind(widthProperty());
        viewPanel.prefHeightProperty().bind(heightProperty());
        getChildren().setAll(viewPanel, canvas);
        canvas.setMouseTransparent(true);
        drawCurrent();
    }

    @Override
    protected void draw(GraphViewState state) {
        if (graphDisposed) {
            return;
        }
        synchronizeGraph(state);
        clear();
        drawAccentFrame();
        drawTransientFeedbackOverlay();
    }

    private void synchronizeGraph(GraphViewState state) {
        Set<String> desiredNodeIds = new LinkedHashSet<>();
        for (int id : state.graph().nodes()) {
            desiredNodeIds.add(String.valueOf(id));
        }

        Map<String, IntEdge> desiredEdges = edgeIds(state.graph().edges());
        List<String> obsoleteEdges = graph.edges()
                .map(Edge::getId)
                .filter(id -> !desiredEdges.containsKey(id))
                .toList();
        for (String edgeId : obsoleteEdges) {
            graph.removeEdge(edgeId);
        }

        List<String> obsoleteNodes = graph.nodes()
                .map(Node::getId)
                .filter(id -> !desiredNodeIds.contains(id))
                .toList();
        for (String nodeId : obsoleteNodes) {
            graph.removeNode(nodeId);
        }

        for (int id : state.graph().nodes()) {
            String nodeId = String.valueOf(id);
            Node node = graph.getNode(nodeId);
            if (node == null) {
                node = graph.addNode(nodeId);
            }
            node.setAttribute("ui.label", nodeId);
            applyNodeClass(node, id, state);
        }

        for (Map.Entry<String, IntEdge> entry : desiredEdges.entrySet()) {
            Edge graphEdge = graph.getEdge(entry.getKey());
            if (graphEdge == null) {
                IntEdge edge = entry.getValue();
                graphEdge = graph.addEdge(entry.getKey(), String.valueOf(edge.from()), String.valueOf(edge.to()), true);
            }
            applyEdgeClass(graphEdge, entry.getValue(), state);
        }
    }

    private void applyEdgeClass(Edge edge, IntEdge value, GraphViewState state) {
        String style = null;
        Integer parent = state.parents().get(value.to());
        if (parent != null && parent == value.from()) {
            style = "fill-color: #FFD700; size: 7px;";
        }
        if (value.equals(state.examinedEdge())) {
            style = "fill-color: #00A2FF; size: 9px;";
        }
        if (style == null) {
            edge.removeAttribute("ui.style");
        } else {
            edge.setAttribute("ui.style", style);
        }
    }

    private Map<String, IntEdge> edgeIds(List<IntEdge> edges) {
        Map<String, IntEdge> result = new LinkedHashMap<>();
        Map<EdgePair, Integer> occurrences = new HashMap<>();
        for (IntEdge edge : edges) {
            EdgePair pair = new EdgePair(edge.from(), edge.to());
            int occurrence = occurrences.getOrDefault(pair, 0);
            occurrences.put(pair, occurrence + 1);
            result.put("e:" + edge.from() + ":" + edge.to() + ":" + occurrence, edge);
        }
        return result;
    }

    private void applyNodeClass(Node node, int id, GraphViewState state) {
        String styleClass = null;
        if (state.discovered().contains(id)) {
            styleClass = "secondary";
        }
        if (state.entered().contains(id)) {
            styleClass = "highlight";
        }
        if (state.visited().contains(id)) {
            styleClass = "visited";
        }
        if (state.focus() != null && state.focus() == id) {
            styleClass = "highlight";
        }
        if (styleClass == null) {
            node.removeAttribute("ui.class");
        } else {
            node.setAttribute("ui.class", styleClass);
        }
    }

    @Override
    public void clear() {
        gc.clearRect(0.0d, 0.0d, canvas.getWidth(), canvas.getHeight());
    }

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        accentUntilMillis = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
    }

    @Override
    public void onVisualizationReset() {
        accentUntilMillis = 0L;
        super.onVisualizationReset();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        accentUntilMillis = 0L;
        if (viewer != null) {
            viewer.disableAutoLayout();
        }
        super.onModuleDetached(moduleId);
        clear();
    }

    @Override
    public void onModuleAttached(String moduleId) {
        super.onModuleAttached(moduleId);
        if (viewer != null) {
            viewer.enableAutoLayout();
        }
    }

    private void drawAccentFrame() {
        long remaining = accentUntilMillis - System.currentTimeMillis();
        if (remaining <= 0L) {
            return;
        }
        gc.save();
        gc.setGlobalAlpha(Math.min(1.0d, remaining / (double) FEEDBACK_DURATION_MS));
        gc.setStroke(RAN_CYAN);
        gc.setLineWidth(2.5d);
        gc.strokeRoundRect(12.0d, 12.0d,
                Math.max(0.0d, canvas.getWidth() - 24.0d),
                Math.max(0.0d, canvas.getHeight() - 24.0d), 18.0d, 18.0d);
        gc.restore();
    }

    @Override
    public void dispose() {
        if (graphDisposed) {
            return;
        }
        graphDisposed = true;
        if (viewPanel != null) {
            viewPanel.prefWidthProperty().unbind();
            viewPanel.prefHeightProperty().unbind();
            viewPanel = null;
        }
        if (viewer != null) {
            viewer.close();
            viewer = null;
        }
        graph.clear();
        super.dispose();
    }

    private record EdgePair(int from, int to) {
    }
}
