package com.majortom.algorithms.visualization.impl.visualizer.hash;

import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.api.EdgeGeometry;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.layout.LayoutEngine;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic bucket-row layout independent of collision strategy. */
public final class HashTableLayout implements LayoutEngine {
  public static final String ID = "hash-table";

  @Override
  public String id() {
    return ID;
  }

  @Override
  public LayoutResult layout(LayoutRequest request) {
    Map<String, LayoutElement> input = new LinkedHashMap<>();
    for (LayoutElement element : request.elements()) {
      input.put(element.id(), element);
    }

    Map<String, LayoutLink> nextBySource = new LinkedHashMap<>();
    for (LayoutLink link : request.links().stream()
        .sorted(Comparator.comparingInt(LayoutLink::order)).toList()) {
      nextBySource.putIfAbsent(link.sourceId(), link);
    }

    List<LayoutElement> buckets = request.elements().stream()
        .filter(element -> HashVisualIds.isBucket(element.id()))
        .sorted(Comparator.comparingInt(element -> HashVisualIds.bucketIndex(element.id())))
        .toList();

    Map<String, ElementGeometry> elements = new LinkedHashMap<>();
    double y = HashLayoutMetrics.LAYOUT_PADDING;
    double maxX = HashLayoutMetrics.LAYOUT_PADDING;
    for (LayoutElement bucket : buckets) {
      double x = HashLayoutMetrics.LAYOUT_PADDING;
      elements.put(bucket.id(), new ElementGeometry(
          bucket.id(), x, y, bucket.width(), bucket.height()));
      x += bucket.width() + HashLayoutMetrics.ENTRY_GAP;

      String current = bucket.id();
      while (nextBySource.containsKey(current)) {
        LayoutLink link = nextBySource.get(current);
        LayoutElement entry = input.get(link.targetId());
        if (entry == null || elements.containsKey(entry.id())) {
          break;
        }
        elements.put(entry.id(), new ElementGeometry(
            entry.id(), x, y, entry.width(), entry.height()));
        x += entry.width() + HashLayoutMetrics.ENTRY_GAP;
        current = entry.id();
      }
      maxX = Math.max(maxX, x - HashLayoutMetrics.ENTRY_GAP);
      y += Math.max(HashLayoutMetrics.NODE_HEIGHT, bucket.height()) + HashLayoutMetrics.ROW_GAP;
    }

    List<EdgeGeometry> edges = new ArrayList<>();
    for (LayoutLink link : request.links()) {
      ElementGeometry source = elements.get(link.sourceId());
      ElementGeometry target = elements.get(link.targetId());
      if (source == null || target == null) continue;
      edges.add(new EdgeGeometry(link.id(), List.of(
          center(source), center(target))));
    }

    double width = Math.max(0.0d, maxX - HashLayoutMetrics.LAYOUT_PADDING);
    double height = buckets.isEmpty()
        ? 0.0d
        : Math.max(0.0d, y - HashLayoutMetrics.ROW_GAP - HashLayoutMetrics.LAYOUT_PADDING);
    return new LayoutResult(
        request.requestId(),
        request.modelRevision(),
        elements,
        edges,
        new BoundsSnapshot(
            HashLayoutMetrics.LAYOUT_PADDING,
            HashLayoutMetrics.LAYOUT_PADDING,
            width,
            height));
  }

  private static EdgeGeometry.Point center(ElementGeometry geometry) {
    return new EdgeGeometry.Point(
        geometry.x() + geometry.width() / 2.0d,
        geometry.y() + geometry.height() / 2.0d);
  }
}
