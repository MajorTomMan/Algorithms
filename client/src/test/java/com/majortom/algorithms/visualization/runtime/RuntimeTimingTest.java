package com.majortom.algorithms.visualization.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.logging.LogLevel;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.render.timing.RenderTimer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class RuntimeTimingTest {
  @Test
  void liveQueueUsesSharedTimerAndPreservesPauseStepResume() {
    ManualTimer timer = new ManualTimer();
    List<Long> consumed = new ArrayList<>();
    JavaFxEventSink sink = new JavaFxEventSink(
        Runnable::run, event -> consumed.add(event.sequence()), 16, () -> 25L, timer);

    sink.accept(event(0));
    sink.accept(event(1));
    sink.pause();
    timer.runNext();
    assertTrue(consumed.isEmpty());
    assertEquals(2, sink.pendingEventCount());

    sink.step();
    timer.runNext();
    assertEquals(List.of(0L), consumed);
    assertEquals(1, sink.pendingEventCount());

    sink.resume();
    timer.runNext();
    assertEquals(List.of(0L, 1L), consumed);
    assertTrue(sink.drained().isDone());
    sink.close();
  }

  @Test
  void replayLogicalGenerationInvalidatesAlreadyScheduledFrames() {
    ManualTimer timer = new ManualTimer();
    List<Integer> states = new ArrayList<>();
    PlaybackController<Integer> controller = new PlaybackController<>(
        countingReducer(), states::add, Runnable::run, Duration.ofMillis(100), timer);
    controller.load(List.of(event(0), event(1)));

    controller.play();
    timer.runNext();
    assertEquals(List.of(1), states);
    assertTrue(controller.isPlaying());

    controller.pause();
    assertFalse(controller.isPlaying());
    timer.runNext(); // stale second-frame callback from the previous generation
    assertEquals(List.of(1), states);

    controller.play();
    timer.runNext();
    assertEquals(List.of(1, 2), states);
    assertFalse(controller.isPlaying());
    controller.close();
  }

  private static EventReducer<Integer> countingReducer() {
    return new EventReducer<>() {
      @Override
      public Integer initialState() {
        return 0;
      }
      @Override
      public Reduction<Integer> reduce(Integer previousState, EventEnvelope event) {
        return Reduction.changed(previousState + 1, EventImportance.STATE_CHANGE, true);
      }
    };
  }

  private static EventEnvelope event(long sequence) {
    return new EventEnvelope("run", "op", sequence, Instant.EPOCH.plusMillis(sequence), "test",
        new LogEvent(LogLevel.INFO, "test", "event-" + sequence));
  }

  private static final class ManualTimer implements RenderTimer {
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void schedule(Duration delay, Runnable task) {
      tasks.add(task);
    }

    void runNext() {
      Runnable task = tasks.poll();
      if (task == null)
        throw new AssertionError("No scheduled task");
      task.run();
    }
  }
}
