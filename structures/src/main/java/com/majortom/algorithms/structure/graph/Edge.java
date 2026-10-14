package com.majortom.algorithms.structure.graph;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
/** One independently addressable edge; parallel edges may connect identical endpoints. */
public final class Edge<T> {
  private static final AtomicLong IDS = new AtomicLong(1L);
  private final long id;
  private final Vertex<T> from;
  private final Vertex<T> to;
  private final Object value;
  private double weight;
  public Edge(Vertex<T> from, Vertex<T> to) { this(from, to, null, 1.0); }
  public Edge(Vertex<T> from, Vertex<T> to, double weight) { this(from, to, null, weight); }
  public Edge(long id, Vertex<T> from, Vertex<T> to, double weight) { this(id, from, to, null, weight); }
  public Edge(Vertex<T> from, Vertex<T> to, Object value, double weight) {
    this(IDS.getAndIncrement(), from, to, value, weight);
  }
  public Edge(long id, Vertex<T> from, Vertex<T> to, Object value, double weight) {
    if (id <= 0 || !Double.isFinite(weight)) throw new IllegalArgumentException("invalid edge ID or weight");
    this.id = id;
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
    this.value = value;
    this.weight = weight;
    IDS.accumulateAndGet(id + 1, Math::max);
  }
  public long id() { return id; }
  public Vertex<T> from() { return from; }
  public Vertex<T> to() { return to; }
  public Object value() { return value; }
  public double weight() { return weight; }
  void updateWeight(double value) { weight = value; }
}
