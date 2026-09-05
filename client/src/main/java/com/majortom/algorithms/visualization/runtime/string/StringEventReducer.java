package com.majortom.algorithms.visualization.runtime.string;

import com.majortom.algorithms.core.domain.execution.RunCompletedEvent;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

/** Reduces factual String mutations, KMP observations and Runtime lifecycle into StringViewState. */
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
            return changed(state(value,
                    StringViewState.Mutation.inserted(inserted.index(), inserted.value().length()), previous.patternStart()));
        }
        if (event instanceof StringStructureEvent.Removed removed) {
            String value = remove(previous.value(), removed.index(), removed.value().length());
            return changed(state(value,
                    StringViewState.Mutation.removed(removed.index(), removed.value().length()), previous.patternStart()));
        }
        if (event instanceof StringStructureEvent.Updated updated) {
            char[] chars = previous.value().toCharArray();
            chars[updated.index()] = updated.value();
            return changed(state(new String(chars), StringViewState.Mutation.updated(updated.index()), previous.patternStart()));
        }
        if (event instanceof StringStructureEvent.Replaced replaced) {
            String value = replace(previous.value(), replaced.index(), replaced.previousValue().length(), replaced.value());
            return changed(state(value,
                    StringViewState.Mutation.replaced(replaced.index(), replaced.value().length()), previous.patternStart()));
        }
        if (event instanceof ObservationEvent.Compared compared) {
            Integer targetIndex = index(compared.leftRef(), "target");
            Integer patternIndex = index(compared.rightRef(), "pattern");
            if (targetIndex != null && patternIndex != null) {
                int patternStart = targetIndex - patternIndex;
                return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                        StringViewState.Observation.compared(targetIndex, patternIndex), patternStart, false));
            }
        }
        if (event instanceof ObservationEvent.Matched matched) {
            return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                    StringViewState.Observation.matched(matched.index(), matched.length()), matched.index(), false));
        }
        if (event instanceof ObservationEvent.Fallback fallback) {
            int shift = fallback.fromIndex() - fallback.toIndex();
            int patternStart = previous.patternStart() + shift;
            return observation(new StringViewState(previous.value(), StringViewState.Mutation.none(),
                    StringViewState.Observation.fallback(fallback.fromIndex(), fallback.toIndex()), patternStart, false));
        }
        if (event instanceof RunCompletedEvent) {
            return Reduction.changed(
                    new StringViewState(previous.value(), StringViewState.Mutation.none(),
                            StringViewState.Observation.none(), previous.patternStart(), true),
                    EventImportance.TERMINAL,
                    true);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static StringViewState state(String value, StringViewState.Mutation mutation, int patternStart) {
        return new StringViewState(value, mutation, StringViewState.Observation.none(), patternStart, false);
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
