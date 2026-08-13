package com.majortom.algorithms.core.runtime;

/**
 * Stateless transformation from an authoritative execution event to immutable state.
 * Implementations must derive the result only from {@code previousState} and {@code event}.
 */
public interface EventReducer<S> {

    /** Creates the state used before sequence zero is reduced. */
    S initialState();

    /** Reduces one event without retaining execution-specific mutable state. */
    Reduction<S> reduce(S previousState, ExecutionEvent event);
}
