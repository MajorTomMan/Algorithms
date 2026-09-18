package com.majortom.algorithms.visualization.render.presentation;

import com.majortom.algorithms.core.runtime.ExecutionAnchor;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import java.util.List;
import java.util.Objects;

/** Maps a render presentation cursor back onto the original monotonic execution timeline. */
public final class ExecutionTimeProjector {
  private ExecutionTimeProjector() {}

  public static long project(ExecutionAnchorTimeline timeline, PresentationCursor cursor) {
    Objects.requireNonNull(timeline, "timeline");
    Objects.requireNonNull(cursor, "cursor");
    if (!timeline.runId().equals(cursor.runId())) {
      throw new IllegalArgumentException("cursor runId does not match execution timeline");
    }
    return project(timeline.anchors(), cursor);
  }

  static long project(List<ExecutionAnchor> anchors, PresentationCursor cursor) {
    Objects.requireNonNull(anchors, "anchors");
    Objects.requireNonNull(cursor, "cursor");
    if (cursor.eventSequence() < 0L || anchors.isEmpty()) {
      return 0L;
    }

    int targetIndex = anchorAtOrBefore(anchors, cursor.eventSequence());
    if (targetIndex < 0) {
      return 0L;
    }
    ExecutionAnchor target = anchors.get(targetIndex);
    // A sparse/missing target sequence has no authoritative transition segment. The nearest
    // preceding factual anchor is the safest projection and never invents execution time.
    if (target.eventSequence() != cursor.eventSequence() || targetIndex == 0) {
      return target.elapsedNanos();
    }

    ExecutionAnchor previous = anchors.get(targetIndex - 1);
    long span = Math.max(0L, target.elapsedNanos() - previous.elapsedNanos());
    long interpolated = previous.elapsedNanos()
        + Math.round(span * cursor.intraEventProgress());
    return Math.max(previous.elapsedNanos(), Math.min(target.elapsedNanos(), interpolated));
  }

  private static int anchorAtOrBefore(List<ExecutionAnchor> anchors, long sequence) {
    int low = 0;
    int high = anchors.size() - 1;
    int result = -1;
    while (low <= high) {
      int middle = low + (high - low) / 2;
      if (anchors.get(middle).eventSequence() <= sequence) {
        result = middle;
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    return result;
  }
}
