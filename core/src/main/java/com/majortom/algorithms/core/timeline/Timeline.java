package com.majortom.algorithms.core.timeline;

import com.majortom.algorithms.core.runtime.*;

import java.util.*;

public final class Timeline implements EventSink {

    private final List<EventEnvelope> events = new ArrayList<>();

    public synchronized void accept(EventEnvelope e) {
        events.add(Objects.requireNonNull(e));
    }

    public synchronized int size() {
        return events.size();
    }

    public synchronized boolean isEmpty() {
        return events.isEmpty();
    }

    public synchronized EventEnvelope event(int i) {
        return events.get(i);
    }

    public synchronized List<EventEnvelope> events() {
        return List.copyOf(events);
    }

    public synchronized List<EventEnvelope> run(String id) {
        return events.stream().filter(e -> e.runId().equals(id)).toList();
    }

    public synchronized List<EventEnvelope> operation(String id) {
        return events.stream().filter(e -> e.operationId().equals(id)).toList();
    }

    public synchronized void clear() {
        events.clear();
    }
}
