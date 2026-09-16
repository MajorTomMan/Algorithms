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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase4LayoutEngineTest {
  @Test
  void linearEngineRespectsHorizontalAndVerticalDirections() {
    var elements = List.of(new LayoutElement("a", 40, 20), new LayoutElement("b", 40, 20));
    var horizontal = new LinearLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(10), RenderSessionId.of("QUEUE"), 1, 1,
            LinearLayoutEngine.ID, elements, Map.of("direction", "RIGHT", "spacing", "8")));
    var vertical = new LinearLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(11), RenderSessionId.of("STACK"), 1, 1,
            LinearLayoutEngine.ID, elements, Map.of("direction", "DOWN", "spacing", "8")));

    assertTrue(horizontal.elements().get("b").x() > horizontal.elements().get("a").x());
    assertEquals(horizontal.elements().get("a").y(), horizontal.elements().get("b").y(), 0.001d);
    assertTrue(vertical.elements().get("b").y() > vertical.elements().get("a").y());
    assertEquals(vertical.elements().get("a").x(), vertical.elements().get("b").x(), 0.001d);
  }

  @Test
  void linearEngineKeepsVariableSizedHorizontalElementsPerfectlyLevel() {
    var elements = List.of(
        new LayoutElement("a", 42, 46),
        new LayoutElement("b", 38, 40),
        new LayoutElement("c", 51, 48),
        new LayoutElement("d", 35, 44));
    var result = new LinearLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(14), RenderSessionId.of("ARRAY"), 1, 1,
            LinearLayoutEngine.ID, elements,
            Map.of("direction", "RIGHT", "padding", "16", "spacing", "6")));

    double y = result.elements().get("a").y();
    assertEquals(16.0d, y, 0.001d);
    for (var geometry : result.elements().values()) {
      assertEquals(y, geometry.y(), 0.001d, "horizontal elements must share one factual Y");
    }
    assertEquals(16.0d, result.elements().get("a").x(), 0.001d);
    assertEquals(64.0d, result.elements().get("b").x(), 0.001d);
    assertEquals(108.0d, result.elements().get("c").x(), 0.001d);
    assertEquals(165.0d, result.elements().get("d").x(), 0.001d);
    assertEquals(184.0d, result.bounds().width(), 0.001d);
    assertEquals(48.0d, result.bounds().height(), 0.001d);
  }

  @Test
  void linearEngineKeepsReverseDirectionsDeterministicAndAxisAligned() {
    var elements = List.of(
        new LayoutElement("a", 30, 20),
        new LayoutElement("b", 45, 25),
        new LayoutElement("c", 35, 30));
    var left = new LinearLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(15), RenderSessionId.of("QUEUE"), 1, 1,
            LinearLayoutEngine.ID, elements,
            Map.of("direction", "LEFT", "padding", "10", "spacing", "5")));
    var up = new LinearLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(16), RenderSessionId.of("STACK"), 1, 1,
            LinearLayoutEngine.ID, elements,
            Map.of("direction", "UP", "padding", "10", "spacing", "5")));

    assertTrue(left.elements().get("a").x() > left.elements().get("b").x());
    assertTrue(left.elements().get("b").x() > left.elements().get("c").x());
    assertEquals(left.elements().get("a").y(), left.elements().get("b").y(), 0.001d);
    assertEquals(left.elements().get("a").y(), left.elements().get("c").y(), 0.001d);

    assertTrue(up.elements().get("a").y() > up.elements().get("b").y());
    assertTrue(up.elements().get("b").y() > up.elements().get("c").y());
    assertEquals(up.elements().get("a").x(), up.elements().get("b").x(), 0.001d);
    assertEquals(up.elements().get("a").x(), up.elements().get("c").x(), 0.001d);
  }

  @Test
  void linkedEngineUsesLinksAndReturnsOnlyNodePrimaryBounds() {
    var result = new LinkedListElkLayout().layout(new LayoutRequest(new LayoutRequestId(12),
        RenderSessionId.of("LINKED_LIST"), 3, 1, LinkedListElkLayout.ID,
        List.of(new LayoutElement("linked:1", 80, 44), new LayoutElement("linked:2", 80, 44)),
        List.of(new LayoutLink("next:1", "linked:1", "linked:2")), Map.of()));

    assertEquals(2, result.elements().size());
    assertEquals(1, result.edges().size());
    assertFalse(result.bounds().isEmpty());
    assertTrue(result.bounds().width() >= 160.0d);
  }

  @Test
  void fixedEngineKeepsMazeWorldExtentDeterministic() {
    var result = new FixedLayoutEngine().layout(
        new LayoutRequest(new LayoutRequestId(13), RenderSessionId.of("MAZE"), 2, 1,
            FixedLayoutEngine.ID, List.of(new LayoutElement("maze:grid", 320, 224)), Map.of()));

    assertEquals(0.0d, result.elements().get("maze:grid").x(), 0.001d);
    assertEquals(0.0d, result.elements().get("maze:grid").y(), 0.001d);
    assertEquals(320.0d, result.bounds().width(), 0.001d);
    assertEquals(224.0d, result.bounds().height(), 0.001d);
  }
}
