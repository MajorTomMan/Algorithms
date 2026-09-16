package com.majortom.algorithms.visualization.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.majortom.algorithms.visualization.impl.visualizer.linked.LinkedListLayout;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutRequestId;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LinkedListDeterministicLayoutTest {
  @Test
  void nodesStayLevelAndEdgesUseExactAdjacentEndpoints() {
    var result = new LinkedListLayout().layout(new LayoutRequest(new LayoutRequestId(31),
        RenderSessionId.of("LINKED_LIST"), 1, 1, LinkedListLayout.ID,
        List.of(new LayoutElement("linked:1", 80, 44), new LayoutElement("linked:2", 96, 44),
            new LayoutElement("linked:3", 72, 44)),
        List.of(new LayoutLink("next:1", "linked:1", "linked:2"),
            new LayoutLink("next:2", "linked:2", "linked:3")),
        Map.of()));

    var first = result.elements().get("linked:1");
    var second = result.elements().get("linked:2");
    var third = result.elements().get("linked:3");
    assertEquals(34.0d, first.x(), 0.001d);
    assertEquals(160.0d, second.x(), 0.001d);
    assertEquals(302.0d, third.x(), 0.001d);
    assertEquals(first.y(), second.y(), 0.001d);
    assertEquals(first.y(), third.y(), 0.001d);

    var route = result.edges().getFirst();
    assertEquals(114.0d, route.points().getFirst().x(), 0.001d);
    assertEquals(56.0d, route.points().getFirst().y(), 0.001d);
    assertEquals(160.0d, route.points().getLast().x(), 0.001d);
    assertEquals(56.0d, route.points().getLast().y(), 0.001d);
    assertEquals(34.0d, result.bounds().minX(), 0.001d);
    assertEquals(34.0d, result.bounds().minY(), 0.001d);
    assertEquals(340.0d, result.bounds().width(), 0.001d);
    assertEquals(44.0d, result.bounds().height(), 0.001d);
  }
}
