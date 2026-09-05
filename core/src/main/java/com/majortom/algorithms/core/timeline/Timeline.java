package com.majortom.algorithms.core.timeline;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.EventSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Timeline implements EventSink {
    private final List<EventEnvelope> events = new ArrayList<>();

    @Override
    public synchronized void accept(EventEnvelope event) {
        events.add(Objects.requireNonNull(event, "event"));
    }

    public synchronized int size() {
        return events.size();
    }

    public synchronized boolean isEmpty() {
        return events.isEmpty();
    }

    public synchronized EventEnvelope event(int index) {
        return events.get(index);
    }

    public synchronized List<EventEnvelope> events() {
        return List.copyOf(events);
    }

    public synchronized List<EventEnvelope> run(String runId) {
        return events.stream()
                .filter(event -> event.runId().equals(runId))
                .toList();
    }

    public synchronized List<EventEnvelope> operation(String operationId) {
        return events.stream()
                .filter(event -> event.operationId().equals(operationId))
                .toList();
    }

    public synchronized void clear() {
        events.clear();
    }
}
