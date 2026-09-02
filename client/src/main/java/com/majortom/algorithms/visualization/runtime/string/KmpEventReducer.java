package com.majortom.algorithms.visualization.runtime.string;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.library.string.KmpEvent;

import java.util.ArrayList;
import java.util.List;

public final class KmpEventReducer implements EventReducer<StringViewState> {
    private final String target;
    private final String pattern;

    public KmpEventReducer(String target, String pattern) {
        this.target = target;
        this.pattern = pattern;
    }

    @Override
    public StringViewState initialState() {
        return new StringViewState(target, pattern, -1, -1, List.of(), StringViewState.Phase.IDLE, false);
    }

    @Override
    public Reduction<StringViewState> reduce(StringViewState previous, EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof KmpEvent.Initialized initialized) {
            return changed(new StringViewState(initialized.target(), initialized.pattern(), -1, -1,
                    List.of(), StringViewState.Phase.INITIALIZED, false), EventImportance.CHECKPOINT);
        }
        if (event instanceof KmpEvent.Compared compared) {
            return changed(copy(previous, compared.targetIndex(), compared.patternIndex(), previous.matches(),
                    StringViewState.Phase.COMPARING, false), EventImportance.TRANSIENT);
        }
        if (event instanceof KmpEvent.Fallback fallback) {
            return changed(copy(previous, fallback.targetIndex(), fallback.toPatternIndex(), previous.matches(),
                    StringViewState.Phase.FALLBACK, false), EventImportance.TRANSIENT);
        }
        if (event instanceof KmpEvent.Matched matched) {
            List<Integer> matches = new ArrayList<>(previous.matches());
            matches.add(matched.startIndex());
            return changed(copy(previous, matched.startIndex(), 0, matches,
                    StringViewState.Phase.MATCHED, false), EventImportance.STATE_CHANGE);
        }
        if (event instanceof KmpEvent.Completed completed) {
            return changed(copy(previous, -1, -1, completed.matchPositions(),
                    StringViewState.Phase.COMPLETED, true), EventImportance.TERMINAL);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static StringViewState copy(StringViewState previous, int targetIndex, int patternIndex,
            List<Integer> matches, StringViewState.Phase phase, boolean completed) {
        return new StringViewState(previous.target(), previous.pattern(), targetIndex, patternIndex, matches, phase, completed);
    }

    private static Reduction<StringViewState> changed(StringViewState state, EventImportance importance) {
        return Reduction.changed(state, importance, true);
    }
}
