package com.majortom.algorithms.visualization.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListElkLayout;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutRequestId;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.layout.FixedLayoutEngine;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class Phase4LayoutEngineTest {
    @Test
    void linearEngineRespectsHorizontalAndVerticalDirections() {
        var elements = List.of(new LayoutElement("a", 40, 20), new LayoutElement("b", 40, 20));
        var horizontal =
                new LinearLayoutEngine()
                        .layout(
                                new LayoutRequest(
                                        new LayoutRequestId(10),
                                        RenderSessionId.of("QUEUE"),
                                        1,
                                        1,
                                        LinearLayoutEngine.ID,
                                        elements,
                                        Map.of("direction", "RIGHT", "spacing", "8")));
        var vertical =
                new LinearLayoutEngine()
                        .layout(
                                new LayoutRequest(
                                        new LayoutRequestId(11),
                                        RenderSessionId.of("STACK"),
                                        1,
                                        1,
                                        LinearLayoutEngine.ID,
                                        elements,
                                        Map.of("direction", "DOWN", "spacing", "8")));

        assertTrue(horizontal.elements().get("b").x() > horizontal.elements().get("a").x());
        assertEquals(
                horizontal.elements().get("a").y(), horizontal.elements().get("b").y(), 0.001d);
        assertTrue(vertical.elements().get("b").y() > vertical.elements().get("a").y());
        assertEquals(vertical.elements().get("a").x(), vertical.elements().get("b").x(), 0.001d);
    }

    @Test
    void linkedEngineUsesLinksAndReturnsOnlyNodePrimaryBounds() {
        var result =
                new LinkedListElkLayout()
                        .layout(
                                new LayoutRequest(
                                        new LayoutRequestId(12),
                                        RenderSessionId.of("LINKED_LIST"),
                                        3,
                                        1,
                                        LinkedListElkLayout.ID,
                                        List.of(
                                                new LayoutElement("linked:1", 80, 44),
                                                new LayoutElement("linked:2", 80, 44)),
                                        List.of(new LayoutLink("next:1", "linked:1", "linked:2")),
                                        Map.of()));

        assertEquals(2, result.elements().size());
        assertEquals(1, result.edges().size());
        assertFalse(result.bounds().isEmpty());
        assertTrue(result.bounds().width() >= 160.0d);
    }

    @Test
    void fixedEngineKeepsMazeWorldExtentDeterministic() {
        var result =
                new FixedLayoutEngine()
                        .layout(
                                new LayoutRequest(
                                        new LayoutRequestId(13),
                                        RenderSessionId.of("MAZE"),
                                        2,
                                        1,
                                        FixedLayoutEngine.ID,
                                        List.of(new LayoutElement("maze:grid", 320, 224)),
                                        Map.of()));

        assertEquals(0.0d, result.elements().get("maze:grid").x(), 0.001d);
        assertEquals(0.0d, result.elements().get("maze:grid").y(), 0.001d);
        assertEquals(320.0d, result.bounds().width(), 0.001d);
        assertEquals(224.0d, result.bounds().height(), 0.001d);
    }
}
