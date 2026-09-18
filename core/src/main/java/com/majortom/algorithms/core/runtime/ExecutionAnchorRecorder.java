package com.majortom.algorithms.core.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/** Records authoritative execution events against a monotonic clock, independent of wall time. */
public final class ExecutionAnchorRecorder implements EventSink {
  private final LongSupplier nanoTimeSource;
  private final List<ExecutionAnchor> anchors = new ArrayList<>();
  private String runId;
  private long originNanos;
  private long previousElapsedNanos;

  public ExecutionAnchorRecorder() {
    this(System::nanoTime);
  }

  public ExecutionAnchorRecorder(LongSupplier nanoTimeSource) {
    this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
  }

  @Override
  public synchronized void accept(EventEnvelope event) {
    Objects.requireNonNull(event, "event");
    long now = nanoTimeSource.getAsLong();
    if (runId == null) {
      runId = event.runId();
      originNanos = now;
    } else if (!runId.equals(event.runId())) {
      throw new IllegalArgumentException("Cannot record anchors from multiple runs");
    }

    long expectedSequence = anchors.isEmpty() ? 0L : anchors.getLast().eventSequence() + 1L;
    if (event.sequence() != expectedSequence) {
      throw new IllegalArgumentException(
          "Execution anchor sequence must be contiguous: expected " + expectedSequence
              + " but was " + event.sequence());
    }

    long elapsed = anchors.isEmpty() ? 0L : Math.max(0L, now - originNanos);
    elapsed = Math.max(previousElapsedNanos, elapsed);
    anchors.add(new ExecutionAnchor(event.sequence(), elapsed));
    previousElapsedNanos = elapsed;
  }

  public synchronized Optional<ExecutionAnchorTimeline> snapshot() {
    if (runId == null) {
      return Optional.empty();
    }
    return Optional.of(new ExecutionAnchorTimeline(runId, List.copyOf(anchors)));
  }
}
