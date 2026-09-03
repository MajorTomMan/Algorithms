package com.majortom.algorithms.visualization.runtime.array;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.List;

/** Reduces factual Array mutations and Runtime lifecycle events into ArrayViewState. */
public final class ArrayEventReducer implements EventReducer<ArrayViewState> {
    private final List<Integer> initialValues;

    public ArrayEventReducer() {
        this(List.of());
    }

    public ArrayEventReducer(List<Integer> initialValues) {
        this.initialValues = List.copyOf(initialValues);
    }

    @Override
    public ArrayViewState initialState() {
        return ArrayViewState.source(initialValues);
    }

    @Override
    public Reduction<ArrayViewState> reduce(ArrayViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof ArrayStructureEvent.Inserted inserted) {
            List<Integer> values = new ArrayList<>(previous.values());
            values.add(inserted.index(), (Integer) inserted.value());
            return changed(new ArrayViewState(values, ArrayViewState.Mutation.inserted(inserted.index()), false));
        }
        if (event instanceof ArrayStructureEvent.Removed removed) {
            List<Integer> values = new ArrayList<>(previous.values());
            values.remove(removed.index());
            return changed(new ArrayViewState(values, ArrayViewState.Mutation.removed(removed.index()), false));
        }
        if (event instanceof ArrayStructureEvent.Updated updated) {
            List<Integer> values = new ArrayList<>(previous.values());
            values.set(updated.index(), (Integer) updated.value());
            return changed(new ArrayViewState(values, ArrayViewState.Mutation.updated(updated.index()), false));
        }
        if (event instanceof ArrayStructureEvent.Swapped swapped) {
            List<Integer> values = new ArrayList<>(previous.values());
            values.set(swapped.leftIndex(), (Integer) swapped.leftValue());
            values.set(swapped.rightIndex(), (Integer) swapped.rightValue());
            return changed(new ArrayViewState(
                    values,
                    ArrayViewState.Mutation.swapped(swapped.leftIndex(), swapped.rightIndex()),
                    false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(
                    new ArrayViewState(previous.values(), ArrayViewState.Mutation.none(), true),
                    EventImportance.TERMINAL,
                    true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static Reduction<ArrayViewState> changed(ArrayViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }
}
