package com.majortom.algorithms.visualization.render.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.majortom.algorithms.core.runtime.ExecutionAnchor;
import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionTimeProjectorTest {
  private static final ExecutionAnchorTimeline TIMELINE = new ExecutionAnchorTimeline(
      "run-1",
      List.of(
          new ExecutionAnchor(0L, 0L),
          new ExecutionAnchor(1L, 10_000_000L),
          new ExecutionAnchor(2L, 24_000_000L),
          new ExecutionAnchor(3L, 25_000_000L)));

  @Test
  void mapsTransitionProgressTowardTargetEventOnOriginalExecutionTime() {
    PresentationCursor cursor = new PresentationCursor(
        "run-1", 3L, 0.5d, PresentationCursor.Mode.REPLAY);

    assertEquals(24_500_000L, ExecutionTimeProjector.project(TIMELINE, cursor));
  }

  @Test
  void supportsBeforeStartTransitionStartAndExactEventBoundaries() {
    assertEquals(0L, ExecutionTimeProjector.project(
        TIMELINE, PresentationCursor.beforeStart("run-1", PresentationCursor.Mode.REPLAY)));
    assertEquals(10_000_000L, ExecutionTimeProjector.project(
        TIMELINE, PresentationCursor.transitionStart("run-1", 2L, PresentationCursor.Mode.REPLAY)));
    assertEquals(24_000_000L, ExecutionTimeProjector.project(
        TIMELINE, PresentationCursor.completedEvent("run-1", 2L, PresentationCursor.Mode.REPLAY)));
    assertEquals(24_000_000L, ExecutionTimeProjector.project(
        TIMELINE, PresentationCursor.atEvent("run-1", 2L, PresentationCursor.Mode.REPLAY)));
  }

  @Test
  void rejectsCursorFromAnotherRun() {
    PresentationCursor cursor = PresentationCursor.atEvent(
        "run-2", 1L, PresentationCursor.Mode.REPLAY);
    assertThrows(IllegalArgumentException.class,
        () -> ExecutionTimeProjector.project(TIMELINE, cursor));
  }
}
