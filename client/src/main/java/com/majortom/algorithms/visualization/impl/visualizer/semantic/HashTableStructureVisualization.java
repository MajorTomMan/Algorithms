package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashLayoutMetrics;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashTableLayout;
import com.majortom.algorithms.visualization.impl.visualizer.hash.HashVisualIds;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutLink;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral bucket/entry layout capture. */
public final class HashTableStructureVisualization
    implements StructureVisualization<HashTableViewState> {

  @Override
  public LayoutRequest captureLayout(HashTableViewState state, RenderCaptureContext context) {
    List<LayoutElement> elements = new ArrayList<>();
    List<LayoutLink> links = new ArrayList<>();

    for (HashTableViewState.Bucket bucket : state.buckets()) {
      String bucketId = HashVisualIds.bucket(bucket.index());
      String bucketText = "#" + bucket.index();
      elements.add(new LayoutElement(
          bucketId,
          DetachedMetrics.boxWidth(
              bucketText,
              context.contentStyle(),
              HashLayoutMetrics.MIN_BUCKET_WIDTH,
              HashLayoutMetrics.HORIZONTAL_PADDING),
          HashLayoutMetrics.NODE_HEIGHT));

      String previousId = bucketId;
      int order = 0;
      for (HashTableViewState.Entry entry : bucket.entries()) {
        String entryId = HashVisualIds.entry(entry.id());
        String text = entry.key().text() + " → " + entry.value().text();
        elements.add(new LayoutElement(
            entryId,
            DetachedMetrics.boxWidth(
                text,
                context.contentStyle(),
                HashLayoutMetrics.MIN_ENTRY_WIDTH,
                HashLayoutMetrics.HORIZONTAL_PADDING),
            HashLayoutMetrics.NODE_HEIGHT));
        links.add(new LayoutLink(
            HashVisualIds.link(previousId, entryId),
            previousId,
            entryId,
            "hash-entry",
            order++));
        previousId = entryId;
      }
    }

    return new LayoutRequest(
        context.requestId(),
        context.sessionId(),
        context.modelRevision(),
        context.geometryRevision(),
        HashTableLayout.ID,
        elements,
        links,
        Map.of(LayoutMetadataKeys.STRUCTURE, StructureIds.HASH));
  }
}
