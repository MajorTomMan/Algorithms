package com.majortom.algorithms.library.basic.graph;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Edge<T> {
    private static final AtomicLong IDS = new AtomicLong(1L);

    private final long id;
    private final Vertex<T> from;
    private final Vertex<T> to;

    public Edge(Vertex<T> from, Vertex<T> to) {
        this(IDS.getAndIncrement(), from, to);
    }

    public Edge(long id, Vertex<T> from, Vertex<T> to) {
        if (id <= 0) {
            throw new IllegalArgumentException("edge id must be positive");
        }
        this.id = id;
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        IDS.accumulateAndGet(id + 1L, Math::max);
    }

    public long id() {
        return id;
    }

    public Vertex<T> from() {
        return from;
    }

    public Vertex<T> to() {
        return to;
    }
}
