package com.majortom.algorithms.visualization.runtime.array;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.VisualValue;

import java.util.ArrayList;
import java.util.List;

/** Reduces factual Array mutations, comparisons and Runtime lifecycle into ArrayViewState. */
public final class ArrayEventReducer implements EventReducer<ArrayViewState> {
    private static final String ARRAY_SOURCE = "array";
    private final List<?> initialValues;

    public ArrayEventReducer() { this(List.of()); }
    public ArrayEventReducer(List<?> initialValues) { this.initialValues = List.copyOf(initialValues); }

    @Override
    public ArrayViewState initialState() { return ArrayViewState.source(initialValues); }

    @Override
    public Reduction<ArrayViewState> reduce(ArrayViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof ArrayStructureEvent.Inserted inserted) {
            List<VisualValue> values = new ArrayList<>(previous.values());
            values.add(inserted.index(), VisualValue.of(inserted.value()));
            return changed(state(values, ArrayViewState.Mutation.inserted(inserted.index())));
        }
        if (event instanceof ArrayStructureEvent.Removed removed) {
            List<VisualValue> values = new ArrayList<>(previous.values());
            values.remove(removed.index());
            return changed(state(values, ArrayViewState.Mutation.removed(removed.index())));
        }
        if (event instanceof ArrayStructureEvent.Updated updated) {
            List<VisualValue> values = new ArrayList<>(previous.values());
            values.set(updated.index(), VisualValue.of(updated.value()));
            return changed(state(values, ArrayViewState.Mutation.updated(updated.index())));
        }
        if (event instanceof ArrayStructureEvent.Swapped swapped) {
            List<VisualValue> values = new ArrayList<>(previous.values());
            values.set(swapped.leftIndex(), VisualValue.of(swapped.leftValue()));
            values.set(swapped.rightIndex(), VisualValue.of(swapped.rightValue()));
            return changed(state(values, ArrayViewState.Mutation.swapped(swapped.leftIndex(), swapped.rightIndex())));
        }
        if (event instanceof ObservationEvent.Compared compared) {
            ArrayViewState.Observation observation = observation(compared);
            if (observation.type() != ArrayViewState.ObservationType.NONE) {
                return Reduction.changed(new ArrayViewState(previous.values(), ArrayViewState.Mutation.none(), observation, false),
                        EventImportance.TRANSIENT, true);
            }
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(new ArrayViewState(previous.values(), ArrayViewState.Mutation.none(),
                            ArrayViewState.Observation.none(), true), EventImportance.TERMINAL, true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static ArrayViewState state(List<VisualValue> values, ArrayViewState.Mutation mutation) {
        return new ArrayViewState(values, mutation, ArrayViewState.Observation.none(), false);
    }

    private static ArrayViewState.Observation observation(ObservationEvent.Compared compared) {
        Integer leftIndex = arrayIndex(compared.leftRef());
        Integer rightIndex = arrayIndex(compared.rightRef());
        if (leftIndex != null && rightIndex != null) return ArrayViewState.Observation.comparedIndexes(leftIndex, rightIndex);
        if (leftIndex != null && compared.rightRef() instanceof ObservationEvent.ValueRef valueRef) {
            return ArrayViewState.Observation.comparedValue(leftIndex, valueRef.value());
        }
        if (rightIndex != null && compared.leftRef() instanceof ObservationEvent.ValueRef valueRef) {
            return ArrayViewState.Observation.comparedValue(rightIndex, valueRef.value());
        }
        return ArrayViewState.Observation.none();
    }

    private static Integer arrayIndex(ObservationEvent.Reference reference) {
        if (reference instanceof ObservationEvent.IndexRef indexRef && ARRAY_SOURCE.equals(indexRef.source())) return indexRef.index();
        return null;
    }

    private static Reduction<ArrayViewState> changed(ArrayViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }
}
