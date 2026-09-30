package com.majortom.algorithms.visualization.impl.visualizer.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutRequestId;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HashTableLayoutTest {

  @Test
  void laysBucketsInRowsAndEntriesAsHorizontalChains() {
    String bucket0 = HashVisualIds.bucket(0);
    String bucket1 = HashVisualIds.bucket(1);
    String first = HashVisualIds.entry(1);
    String second = HashVisualIds.entry(2);

    LayoutRequest request = new LayoutRequest(
        new LayoutRequestId(1),
        RenderSessionId.of("HASH_TEST"),
        1L,
        1L,
        HashTableLayout.ID,
        List.of(
            new LayoutElement(bucket0, 64.0d, 52.0d),
            new LayoutElement(first, 132.0d, 52.0d),
            new LayoutElement(second, 132.0d, 52.0d),
            new LayoutElement(bucket1, 64.0d, 52.0d)),
        List.of(
            new LayoutLink(HashVisualIds.link(bucket0, first), bucket0, first, "hash-entry", 0),
            new LayoutLink(HashVisualIds.link(first, second), first, second, "hash-entry", 1)),
        Map.of());

    var result = new HashTableLayout().layout(request);
    ElementGeometry b0 = result.elements().get(bucket0);
    ElementGeometry b1 = result.elements().get(bucket1);
    ElementGeometry e1 = result.elements().get(first);
    ElementGeometry e2 = result.elements().get(second);

    assertEquals(b0.y(), e1.y());
    assertEquals(e1.y(), e2.y());
    assertTrue(e1.x() > b0.x());
    assertTrue(e2.x() > e1.x());
    assertTrue(b1.y() > b0.y());
    assertEquals(2, result.edges().size());
    assertTrue(result.bounds().width() > b0.width());
    assertTrue(result.bounds().height() > b0.height());
  }
}
