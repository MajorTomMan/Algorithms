package com.majortom.algorithms.telemetry.timeline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.telemetry.api.TelemetrySample;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TimelineProjectorTest {
  @Test
  void replayCursorProjectsThroughOriginalExecutionAnchorsInsteadOfPlaybackPercentage() {
    Instant start = Instant.parse("2026-09-18T00:00:00Z");
    List<EventEnvelope> events = List.of(
        event(0, start),
        event(1, start.plusMillis(2)),
        event(2, start.plusMillis(7)),
        event(3, start.plusMillis(42)));
    ExecutionAnchorTimeline anchors = ExecutionAnchorTimeline.fromEvents(events);
    List<TelemetrySample> samples = List.of(
        sample(0, 0),
        sample(10, 100),
        sample(20, 300),
        sample(40, 900),
        sample(50, 1_000));

    TelemetryProjection projection = TimelineProjector.project(
        anchors,
        samples,
        new PresentationCursor("run-1", 2L, 0.5d, PresentationCursor.Mode.REPLAY));

    assertEquals(24_500_000L, projection.executionElapsedNanos());
    assertEquals(2, projection.sampleIndex());
    assertEquals(0.225d, projection.sampleProgress(), 0.000001d);
  }

  @Test
  void scrubBeforeFirstEventProjectsToTimelineOrigin() {
    Instant start = Instant.parse("2026-09-18T00:00:00Z");
    ExecutionAnchorTimeline anchors = ExecutionAnchorTimeline.fromEvents(List.of(event(0, start)));
    TelemetryProjection projection = TimelineProjector.project(
        anchors,
        List.of(sample(0, 0)),
        PresentationCursor.beforeStart("run-1", PresentationCursor.Mode.REPLAY));

    assertEquals(0L, projection.executionElapsedNanos());
    assertEquals(0, projection.sampleIndex());
  }

  private static EventEnvelope event(long sequence, Instant timestamp) {
    return new EventEnvelope("run-1", "op-1", sequence, timestamp, "test", new TestEvent());
  }

  private static TelemetrySample sample(long millis, long value) {
    return new TelemetrySample(
        millis * 1_000_000L,
        Map.of("memory.allocated", TelemetryValue.of(value)));
  }

  private record TestEvent() implements ExecutionEvent {}
}
