package com.majortom.algorithms.core.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thread-safe in-memory event collector for replay, tests, and local runs. */
public final class InMemoryEventSink implements EventSink {

    private final List<EventEnvelope> events = new ArrayList<>();

    @Override
    public synchronized void accept(EventEnvelope event) {
        events.add(Objects.requireNonNull(event, "event"));
    }

    public synchronized List<EventEnvelope> events() {
        return List.copyOf(events);
    }
}
