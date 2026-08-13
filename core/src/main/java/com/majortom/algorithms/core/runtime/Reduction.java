package com.majortom.algorithms.core.runtime;

import java.util.Objects;

/**
 * Result of reducing one authoritative execution event.
 *
 * @param state resulting immutable state
 * @param stateChanged whether the event changed the represented state
 * @param importance retention and presentation importance of the event
 * @param visualFrame whether observers should expose the result as a visible timeline frame
 */
public record Reduction<S>(
        S state,
        boolean stateChanged,
        EventImportance importance,
        boolean visualFrame) {

    public Reduction {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(importance, "importance");
        if (visualFrame && !stateChanged) {
            throw new IllegalArgumentException("A visual frame must represent a state change");
        }
    }

    /** Creates a state-changing reduction result. */
    public static <S> Reduction<S> changed(
            S state,
            EventImportance importance,
            boolean visualFrame) {
        return new Reduction<>(state, true, importance, visualFrame);
    }

    /** Creates a state-preserving reduction result that does not create a visual frame. */
    public static <S> Reduction<S> unchanged(S state, EventImportance importance) {
        return new Reduction<>(state, false, importance, false);
    }
}
