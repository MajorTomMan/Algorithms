package com.majortom.algorithms.visualization.render.presentation.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.core.runtime.ExecutionAnchor;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;
import com.majortom.algorithms.telemetry.memory.api.MemoryAllocationSample;
import com.majortom.algorithms.telemetry.memory.api.MemoryCapabilities;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import com.majortom.algorithms.visualization.render.api.PresentationSnapshotContext;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemoryPresentationSourceTest {
  private static final MemoryCapabilities CAPABILITIES =
      new MemoryCapabilities(true, true, true, true);

  @Test
  void projectsAnimationCursorAndInterpolatesAllocationWithoutMutatingFacts() {
    MemoryFacts facts = facts();
    ExecutionAnchorTimeline anchors = anchors();
    MemoryPresentationSource source = new MemoryPresentationSource(new MemoryPresentationInput(
        TelemetryDomain.ALGORITHM,
        CAPABILITIES,
        Optional.of(facts),
        Optional.of(anchors),
        Optional.empty(),
        Optional.empty(),
        false,
        false,
        false));

    PresentationCursor halfwayToEventThree = new PresentationCursor(
        "run-1", 3L, 0.5d, PresentationCursor.Mode.REPLAY);
    MemoryPresentationModel model = source.snapshot(context(Optional.of(halfwayToEventThree)));

    assertTrue(model.cursorProjected());
    assertEquals(25_000_000L, model.visibleElapsedNanos());
    assertTrue(model.currentAllocatedBytes().isPresent());
    assertEquals(250L, model.currentAllocatedBytes().getAsLong());
    assertEquals(40_000_000L, facts.durationNanos());
    assertEquals(400L, facts.allocatedBytesValue().orElseThrow());
  }

  @Test
  void exactEventCursorProjectsToThatEventAnchor() {
    MemoryPresentationSource source = sourceWithFactsAndAnchors();

    MemoryPresentationModel model = source.snapshot(context(Optional.of(
        PresentationCursor.completedEvent("run-1", 2L, PresentationCursor.Mode.REPLAY))));

    assertTrue(model.cursorProjected());
    assertEquals(20_000_000L, model.visibleElapsedNanos());
    assertEquals(200L, model.currentAllocatedBytes().orElseThrow());
  }

  @Test
  void fallsBackToFullProfileWhenCursorBelongsToAnotherRun() {
    MemoryPresentationSource source = sourceWithFactsAndAnchors();

    MemoryPresentationModel model = source.snapshot(context(Optional.of(
        PresentationCursor.completedEvent("run-other", 2L, PresentationCursor.Mode.REPLAY))));

    assertFalse(model.cursorProjected());
    assertEquals(40_000_000L, model.visibleElapsedNanos());
    assertEquals(400L, model.currentAllocatedBytes().orElseThrow());
  }

  private static MemoryPresentationSource sourceWithFactsAndAnchors() {
    return new MemoryPresentationSource(new MemoryPresentationInput(
        TelemetryDomain.ALGORITHM,
        CAPABILITIES,
        Optional.of(facts()),
        Optional.of(anchors()),
        Optional.empty(),
        Optional.empty(),
        false,
        false,
        false));
  }

  private static PresentationSnapshotContext context(Optional<PresentationCursor> cursor) {
    return new PresentationSnapshotContext(
        RenderSessionId.of("memory:test"), 1L, 1L, cursor, cursor.isPresent() ? 1L : 0L);
  }

  private static MemoryFacts facts() {
    return new MemoryFacts(
        new TelemetrySessionId(7L, TelemetryScopeId.algorithm("array", "bubble-sort")),
        TelemetrySessionState.COMPLETED,
        true,
        40_000_000L,
        400L,
        10_000L,
        10_000L,
        32L,
        1L,
        2L,
        List.of(
            new MemoryAllocationSample(0L, 0L),
            new MemoryAllocationSample(10_000_000L, 100L),
            new MemoryAllocationSample(20_000_000L, 200L),
            new MemoryAllocationSample(30_000_000L, 300L),
            new MemoryAllocationSample(40_000_000L, 400L)));
  }

  private static ExecutionAnchorTimeline anchors() {
    return new ExecutionAnchorTimeline(
        "run-1",
        List.of(
            new ExecutionAnchor(0L, 0L),
            new ExecutionAnchor(1L, 10_000_000L),
            new ExecutionAnchor(2L, 20_000_000L),
            new ExecutionAnchor(3L, 30_000_000L),
            new ExecutionAnchor(4L, 40_000_000L)));
  }
}
