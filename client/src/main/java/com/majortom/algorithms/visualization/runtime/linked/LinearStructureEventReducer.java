package com.majortom.algorithms.visualization.runtime.linked;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reduces mutations exposed by StackStructure/QueueStructure into their linear presentation. */
public final class LinearStructureEventReducer implements EventReducer<LinearStructureViewState> {
  private final LinearStructureViewState initialState;
  private final boolean stack;

  public LinearStructureEventReducer(String kind, List<?> values) {
    Objects.requireNonNull(kind, "kind");
    this.initialState = new LinearStructureViewState(kind, values);
    this.stack = StructureIds.STACK.equals(kind);
    if (!stack && !StructureIds.QUEUE.equals(kind)) {
      throw new IllegalArgumentException("Unsupported linear structure kind: " + kind);
    }
  }

  @Override
  public LinearStructureViewState initialState() {
    return initialState;
  }

  @Override
  public Reduction<LinearStructureViewState> reduce(
      LinearStructureViewState previous, EventEnvelope envelope) {
    Object event = envelope.event();
    if (event instanceof LinkedStructureEvent.NodeInserted inserted) {
      List<Object> values = values(previous);
      if (stack) {
        values.addFirst(inserted.value());
        return changed(
            previous.kind(), values, LinearStructureViewState.Type.PUSH, inserted.value());
      }
      values.add(inserted.value());
      return changed(
          previous.kind(), values, LinearStructureViewState.Type.ENQUEUE, inserted.value());
    }
    if (event instanceof LinkedStructureEvent.NodeRemoved removed) {
      List<Object> values = values(previous);
      if (!values.isEmpty()) {
        values.removeFirst();
      }
      return changed(previous.kind(), values,
          stack ? LinearStructureViewState.Type.POP : LinearStructureViewState.Type.DEQUEUE,
          removed.value());
    }
    return Reduction.unchanged(previous, EventImportance.TRANSIENT);
  }

  private static List<Object> values(LinearStructureViewState state) {
    return new ArrayList<>(state.values().stream().map(value -> value.value()).toList());
  }

  private static Reduction<LinearStructureViewState> changed(
      String kind, List<?> values, LinearStructureViewState.Type type, Object value) {
    return Reduction.changed(LinearStructureViewState.of(
                                 kind, values, LinearStructureViewState.Mutation.of(type, value)),
        EventImportance.STATE_CHANGE, true);
  }
}
