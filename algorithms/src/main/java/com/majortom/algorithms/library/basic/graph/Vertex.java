package com.majortom.algorithms.library.basic.graph;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Vertex<T> {
    private static final AtomicLong IDS = new AtomicLong(1L);

    private final long id;
    private final T value;

    public Vertex(T value) {
        this(IDS.getAndIncrement(), value);
    }

    public Vertex(long id, T value) {
        if (id <= 0) {
            throw new IllegalArgumentException("vertex id must be positive");
        }
        this.id = id;
        this.value = Objects.requireNonNull(value, "value");
        IDS.accumulateAndGet(id + 1L, Math::max);
    }

    public long id() {
        return id;
    }

    public T value() {
        return value;
    }
}
