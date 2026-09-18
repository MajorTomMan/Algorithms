package com.majortom.algorithms.telemetry.timeline;

import com.majortom.algorithms.telemetry.api.TelemetrySample;
import java.util.List;
import java.util.Objects;

/** Maps a live/replay PresentationCursor to original execution time and telemetry sample position. */
public final class TimelineProjector {
  private TimelineProjector() {}

  public static TelemetryProjection project(
      ExecutionAnchorTimeline execution,
      List<TelemetrySample> samples,
      PresentationCursor cursor) {
    Objects.requireNonNull(execution, "execution");
    Objects.requireNonNull(samples, "samples");
    Objects.requireNonNull(cursor, "cursor");
    if (!execution.runId().equals(cursor.runId())) {
      throw new IllegalArgumentException("cursor runId does not match execution timeline");
    }

    long elapsed = executionTime(execution.anchors(), cursor);
    boolean beforeStart = cursor.eventSequence() < 0L;
    if (samples.isEmpty()) {
      return new TelemetryProjection(elapsed, -1, 0.0d, beforeStart, false);
    }

    int sampleIndex = sampleAtOrBefore(samples, elapsed);
    if (sampleIndex < 0) {
      return new TelemetryProjection(elapsed, -1, 0.0d, beforeStart, false);
    }
    if (sampleIndex + 1 >= samples.size()) {
      boolean atEnd = !beforeStart && elapsed >= samples.getLast().elapsedNanos();
      return new TelemetryProjection(elapsed, sampleIndex, beforeStart ? 0.0d : 1.0d, beforeStart, atEnd);
    }

    TelemetrySample left = samples.get(sampleIndex);
    TelemetrySample right = samples.get(sampleIndex + 1);
    long span = Math.max(0L, right.elapsedNanos() - left.elapsedNanos());
    double progress = span == 0L
        ? 1.0d
        : clamp((double) (elapsed - left.elapsedNanos()) / (double) span);
    return new TelemetryProjection(elapsed, sampleIndex, progress, beforeStart, false);
  }

  public static long executionTime(
      List<ExecutionAnchor> anchors,
      PresentationCursor cursor) {
    Objects.requireNonNull(anchors, "anchors");
    Objects.requireNonNull(cursor, "cursor");
    if (cursor.eventSequence() < 0L || anchors.isEmpty()) {
      return 0L;
    }

    int index = anchorAtOrBefore(anchors, cursor.eventSequence());
    if (index < 0) {
      return 0L;
    }
    ExecutionAnchor current = anchors.get(index);
    if (index + 1 >= anchors.size() || current.eventSequence() != cursor.eventSequence()) {
      return current.elapsedNanos();
    }
    ExecutionAnchor next = anchors.get(index + 1);
    long span = Math.max(0L, next.elapsedNanos() - current.elapsedNanos());
    long interpolated = current.elapsedNanos()
        + Math.round(span * cursor.intraEventProgress());
    return Math.max(current.elapsedNanos(), Math.min(next.elapsedNanos(), interpolated));
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

  private static int sampleAtOrBefore(List<TelemetrySample> samples, long elapsedNanos) {
    int low = 0;
    int high = samples.size() - 1;
    int result = -1;
    while (low <= high) {
      int middle = low + (high - low) / 2;
      if (samples.get(middle).elapsedNanos() <= elapsedNanos) {
        result = middle;
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    return result;
  }

  private static double clamp(double value) {
    return Math.max(0.0d, Math.min(1.0d, value));
  }
}
