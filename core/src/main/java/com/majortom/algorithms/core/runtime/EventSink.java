package com.majortom.algorithms.core.runtime;

import java.util.Objects;

/** Receives authoritative execution events. */
@FunctionalInterface
public interface EventSink {

    void accept(EventEnvelope event);

    static EventSink noop() {
        return event -> {
        };
    }

    default EventSink andThen(EventSink next) {
        Objects.requireNonNull(next, "next");
        return event -> {
            accept(event);
            next.accept(event);
        };
    }
}
