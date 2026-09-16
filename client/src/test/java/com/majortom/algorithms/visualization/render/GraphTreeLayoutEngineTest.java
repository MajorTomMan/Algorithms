package com.majortom.algorithms.visualization.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphElkLayout;
import com.majortom.algorithms.visualization.impl.visualizer.tree.TreeElkLayout;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutRequestId;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class GraphTreeLayoutEngineTest {
    @Test
    void graphEngineUsesPureTopologyAndReturnsPrimaryNodeBounds() {
        var request =
                new LayoutRequest(
                        new LayoutRequestId(1),
                        RenderSessionId.of("GRAPH"),
                        4,
                        2,
                        GraphElkLayout.ID,
                        List.of(
                                new LayoutElement("graph:node:1", 56, 56),
                                new LayoutElement("graph:node:2", 56, 56),
                                new LayoutElement("graph:node:3", 56, 56)),
                        List.of(
                                new LayoutLink("graph:edge:1", "graph:node:1", "graph:node:2"),
                                new LayoutLink("graph:edge:2", "graph:node:2", "graph:node:3")),
                        Map.of("directed", "true"));
        var result = new GraphElkLayout().layout(request);
        assertEquals(3, result.elements().size());
        assertFalse(result.bounds().isEmpty());
        assertTrue(result.bounds().width() > 0.0d);
        assertTrue(result.bounds().height() > 0.0d);
    }

    @Test
    void treeEnginePreservesOrderedBinaryRelationships() {
        var request =
                new LayoutRequest(
                        new LayoutRequestId(2),
                        RenderSessionId.of("TREE"),
                        7,
                        1,
                        TreeElkLayout.ID,
                        List.of(
                                new LayoutElement("tree:1", 48, 48),
                                new LayoutElement("tree:2", 48, 48),
                                new LayoutElement("tree:3", 48, 48)),
                        List.of(
                                new LayoutLink("left", "tree:1", "tree:2", "LEFT", 0),
                                new LayoutLink("right", "tree:1", "tree:3", "RIGHT", 1)),
                        Map.of("kind", "BINARY"));
        var result = new TreeElkLayout().layout(request);
        assertEquals(3, result.elements().size());
        assertEquals(2, result.edges().size());
        assertFalse(result.bounds().isEmpty());
    }
}
