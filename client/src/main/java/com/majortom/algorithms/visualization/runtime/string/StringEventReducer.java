package com.majortom.algorithms.visualization.runtime.string;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

/** Reduces factual String mutations, observations and Runtime lifecycle into StringViewState. */
public final class StringEventReducer implements EventReducer<StringViewState> {
    private final String initialValue;

    public StringEventReducer(String initialValue) {
        this.initialValue = initialValue == null ? "" : initialValue;
    }

    @Override
    public StringViewState initialState() {
        return StringViewState.source(initialValue);
    }

    @Override
    public Reduction<StringViewState> reduce(StringViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof StringStructureEvent.Inserted inserted) {
            String value = insert(previous.value(), inserted.index(), inserted.value());
            return changed(new StringViewState(value,
                    StringViewState.Mutation.inserted(inserted.index(), inserted.value().length()),
                    StringViewState.Observation.none(), false));
        }
        if (event instanceof StringStructureEvent.Removed removed) {
            String value = remove(previous.value(), removed.index(), removed.value().length());
            return changed(new StringViewState(value,
                    StringViewState.Mutation.removed(removed.index(), removed.value().length()),
                    StringViewState.Observation.none(), false));
        }
        if (event instanceof StringStructureEvent.Updated updated) {
            char[] chars = previous.value().toCharArray();
            chars[updated.index()] = updated.value();
            return changed(new StringViewState(new String(chars),
                    StringViewState.Mutation.updated(updated.index()),
                    StringViewState.Observation.none(), false));
        }
        if (event instanceof StringStructureEvent.Replaced replaced) {
            String value = replace(previous.value(), replaced.index(), replaced.previousValue().length(), replaced.value());
            return changed(new StringViewState(value,
                    StringViewState.Mutation.replaced(replaced.index(), replaced.value().length()),
                    StringViewState.Observation.none(), false));
        }
        if (event instanceof ObservationEvent.Compared compared) {
            Integer targetIndex = index(compared.leftRef(), "target");
            Integer patternIndex = index(compared.rightRef(), "pattern");
            if (targetIndex != null && patternIndex != null) {
                return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                        StringViewState.Observation.compared(targetIndex, patternIndex), false));
            }
        }
        if (event instanceof ObservationEvent.Matched matched) {
            return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                    StringViewState.Observation.matched(matched.index(), matched.length()), false));
        }
        if (event instanceof ObservationEvent.Fallback fallback) {
            return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                    StringViewState.Observation.fallback(fallback.fromIndex(), fallback.toIndex()), false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(
                    new StringViewState(previous.value(), StringViewState.Mutation.none(),
                            StringViewState.Observation.none(), true),
                    EventImportance.TERMINAL,
                    true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static Integer index(ObservationEvent.Reference reference, String source) {
        if (reference instanceof ObservationEvent.IndexRef indexRef && source.equals(indexRef.source())) {
            return indexRef.index();
        }
        return null;
    }

    private static String insert(String source, int index, String value) {
        return source.substring(0, index) + value + source.substring(index);
    }

    private static String remove(String source, int index, int length) {
        return source.substring(0, index) + source.substring(index + length);
    }

    private static String replace(String source, int index, int length, String value) {
        return source.substring(0, index) + value + source.substring(index + length);
    }

    private static Reduction<StringViewState> changed(StringViewState state) {
        return Reduction.changed(state, EventImportance.STATE_CHANGE, true);
    }

    private static Reduction<StringViewState> observation(StringViewState state) {
        return Reduction.changed(state, EventImportance.TRANSIENT, true);
    }
}
