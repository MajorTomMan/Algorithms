package com.majortom.algorithms.structure.graph;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Edge<T> {
  private static final AtomicLong IDS = new AtomicLong(1L);

  private final long id;
  private final Vertex<T> from;
  private final Vertex<T> to;
  private double weight;

  public Edge(Vertex<T> from, Vertex<T> to) {
    this(from, to, 1.0d);
  }

  public Edge(Vertex<T> from, Vertex<T> to, double weight) {
    this(IDS.getAndIncrement(), from, to, weight);
  }

  public Edge(long id, Vertex<T> from, Vertex<T> to, double weight) {
    if (id <= 0) {
      throw new IllegalArgumentException("edge id must be positive");
    }
    if (!Double.isFinite(weight)) {
      throw new IllegalArgumentException("edge weight must be finite");
    }
    this.id = id;
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
    this.weight = weight;
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

  public double weight() {
    return weight;
  }

  void updateWeight(double weight) {
    this.weight = weight;
  }
}
