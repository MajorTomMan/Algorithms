package com.majortom.algorithms.visualization.render.presentation.memory;

import com.majortom.algorithms.core.runtime.ExecutionAnchorTimeline;
import com.majortom.algorithms.telemetry.memory.api.MemoryAllocationSample;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.visualization.render.api.PresentationCursor;
import com.majortom.algorithms.visualization.render.api.PresentationModelSource;
import com.majortom.algorithms.visualization.render.api.PresentationSnapshotContext;
import com.majortom.algorithms.visualization.render.presentation.ExecutionTimeProjector;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JavaFX-neutral Memory projection. Factual telemetry stays untouched; only the visible presentation
 * window follows the RenderFramework cursor.
 */
public final class MemoryPresentationSource implements PresentationModelSource<MemoryPresentationModel> {
  private final AtomicReference<MemoryPresentationInput> current;

  public MemoryPresentationSource(MemoryPresentationInput initial) {
    current = new AtomicReference<>(Objects.requireNonNull(initial, "initial"));
  }

  public void publish(MemoryPresentationInput input) {
    current.set(Objects.requireNonNull(input, "input"));
  }

  @Override
  public MemoryPresentationModel snapshot(PresentationSnapshotContext context) {
    Objects.requireNonNull(context, "context");
    MemoryPresentationInput input = current.get();
    Optional<MemoryFacts> facts = input.facts();
    List<MemoryAllocationSample> samples = facts.map(MemoryFacts::samples).orElse(List.of());
    long visibleElapsed = facts.map(MemoryFacts::durationNanos).orElse(0L);
    boolean cursorProjected = false;

    if (facts.isPresent() && facts.orElseThrow().timingRepresentative()) {
      Optional<ExecutionAnchorTimeline> anchors = input.executionAnchors();
      Optional<PresentationCursor> cursor = context.cursor();
      if (anchors.isPresent() && cursor.isPresent()
          && anchors.orElseThrow().runId().equals(cursor.orElseThrow().runId())) {
        visibleElapsed = Math.min(
            facts.orElseThrow().durationNanos(),
            ExecutionTimeProjector.project(anchors.orElseThrow(), cursor.orElseThrow()));
        cursorProjected = true;
      }
    }

    OptionalLong currentAllocated = allocationAt(samples, visibleElapsed);
    return new MemoryPresentationModel(
        input.expectedDomain(),
        input.capabilities(),
        facts,
        samples,
        visibleElapsed,
        currentAllocated,
        cursorProjected,
        input.deepAnalysis(),
        input.footprint(),
        input.deepAnalysisEnabled(),
        input.footprintSupported(),
        input.footprintBusy());
  }

  private static OptionalLong allocationAt(List<MemoryAllocationSample> samples, long elapsedNanos) {
    if (samples.isEmpty()) return OptionalLong.empty();
    MemoryAllocationSample first = samples.getFirst();
    if (elapsedNanos <= first.elapsedNanos()) {
      if (first.elapsedNanos() <= 0L) return OptionalLong.of(first.allocatedBytes());
      double ratio = Math.max(0.0d, Math.min(1.0d, (double) elapsedNanos / first.elapsedNanos()));
      return OptionalLong.of(Math.max(0L, Math.round(first.allocatedBytes() * ratio)));
    }
    MemoryAllocationSample previous = first;
    for (int index = 1; index < samples.size(); index++) {
      MemoryAllocationSample next = samples.get(index);
      if (elapsedNanos <= next.elapsedNanos()) {
        long span = next.elapsedNanos() - previous.elapsedNanos();
        if (span <= 0L) return OptionalLong.of(next.allocatedBytes());
        double ratio = (double) (elapsedNanos - previous.elapsedNanos()) / span;
        long bytes = previous.allocatedBytes()
            + Math.round((next.allocatedBytes() - previous.allocatedBytes()) * ratio);
        return OptionalLong.of(Math.max(0L, bytes));
      }
      previous = next;
    }
    return OptionalLong.of(samples.getLast().allocatedBytes());
  }
}
