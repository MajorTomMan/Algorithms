package com.majortom.algorithms.structure.graph;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import java.util.Map;

@Structure(
    id = StructureIds.GRAPH, name = "Graph", module = StructureModule.GRAPH, implementation = Graph.class)
public interface GraphStructure<T> {
  GraphDirection direction();

  default boolean isDirected() {
    return direction().isDirected();
  }

  int vertexCount();
  int edgeCount();

  default boolean isEmpty() {
    return vertexCount() == 0;
  }

  /** Restores a complete graph snapshot without emitting individual structure events. */
  void initialize(GraphSnapshot<T> snapshot);

  Vertex<T> vertex(T value);
  Vertex<T> addVertex(T value);
  boolean removeVertex(Vertex<T> vertex);
  Edge<T> edge(Vertex<T> from, Vertex<T> to);
  Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight);

  default Edge<T> addEdge(Vertex<T> from, Vertex<T> to) {
    return addEdge(from, to, 1.0d);
  }

  boolean removeEdge(Vertex<T> from, Vertex<T> to);
  boolean containsVertex(Vertex<T> vertex);
  boolean containsEdge(Vertex<T> from, Vertex<T> to);
  double weight(Edge<T> edge);
  double setWeight(Edge<T> edge, double weight);
  Iterable<Vertex<T>> vertices();
  Iterable<Edge<T>> edges();
  Iterable<Vertex<T>> neighbors(Vertex<T> vertex);

  /** Returns outgoing neighbors and their actual edge objects in insertion order. */
  Map<Vertex<T>, Edge<T>> adjacentEdges(Vertex<T> vertex);
}
