package com.majortom.algorithms.core.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.domain.execution.RunStartedEvent;
import com.majortom.algorithms.core.event.ExecutionEvent;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ExecutionAnchorRecorderTest {
  @Test
  void recordsMonotonicElapsedTimeForOneRun() {
    AtomicLong clock = new AtomicLong(1_000_000L);
    ExecutionAnchorRecorder recorder = new ExecutionAnchorRecorder(clock::get);

    recorder.accept(event("run-1", 0L, new RunStartedEvent()));
    clock.addAndGet(24_000_000L);
    recorder.accept(event("run-1", 1L, new RunCompletedEvent()));

    ExecutionAnchorTimeline timeline = recorder.snapshot().orElseThrow();
    assertEquals("run-1", timeline.runId());
    assertEquals(2, timeline.anchors().size());
    assertEquals(new ExecutionAnchor(0L, 0L), timeline.anchors().get(0));
    assertEquals(new ExecutionAnchor(1L, 24_000_000L), timeline.anchors().get(1));
    assertEquals(24_000_000L, timeline.durationNanos());
  }

  @Test
  void rejectsMixedRunsAndSequenceGaps() {
    ExecutionAnchorRecorder recorder = new ExecutionAnchorRecorder(() -> 1L);
    recorder.accept(event("run-1", 0L, new RunStartedEvent()));

    assertThrows(IllegalArgumentException.class,
        () -> recorder.accept(event("run-2", 1L, new RunCompletedEvent())));
    assertThrows(IllegalArgumentException.class,
        () -> recorder.accept(event("run-1", 2L, new RunCompletedEvent())));
  }

  private static EventEnvelope event(String runId, long sequence, ExecutionEvent lifecycleEvent) {
    return new EventEnvelope(
        runId,
        "test-operation",
        sequence,
        Instant.EPOCH,
        "test",
        lifecycleEvent);
  }
}
