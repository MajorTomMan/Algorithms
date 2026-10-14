package com.majortom.algorithms.structure.graph;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.List;
@Structure(id = StructureIds.GRAPH, name = "Graph",
    module = StructureModule.GRAPH, implementation = Graph.class)
public interface GraphStructure<T> {
  GraphDirection direction();
  default boolean isDirected() { return direction().isDirected(); }
  default boolean isEmpty() { return vertexCount() == 0; }
  int vertexCount();
  int edgeCount();
  Vertex<T> addVertex(T value);
  Vertex<T> vertex(T value);
  boolean containsVertex(Vertex<T> vertex);
  boolean containsEdge(Vertex<T> from, Vertex<T> to);
  Vertex<T> vertex(long id);
  boolean removeVertex(long id);
  boolean removeVertex(Vertex<T> vertex);
  Edge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value, double weight);
  Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight);
  Edge<T> edge(Vertex<T> from, Vertex<T> to);
  boolean removeEdge(Vertex<T> from, Vertex<T> to);
  double weight(Edge<T> edge);
  double setWeight(Edge<T> edge, double weight);
  Iterable<Vertex<T>> neighbors(Vertex<T> vertex);
  java.util.Map<Vertex<T>, Edge<T>> adjacentEdges(Vertex<T> vertex);
  default Edge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value) {
    return addEdge(from, to, value, 1.0);
  }
  default Edge<T> addEdge(Vertex<T> from, Vertex<T> to) {
    return addEdge(from, to, null, 1.0);
  }
  Graph.BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to,
                                 Object forwardValue, Object reverseValue,
                                 double forwardWeight, double reverseWeight);
  default Graph.BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to, Object value) {
    return addBiEdge(from, to, value, value, 1.0, 1.0);
  }
  Edge<T> edge(long id);
  List<Edge<T>> edges(Vertex<T> from, Vertex<T> to);
  List<Edge<T>> outgoingEdges(Vertex<T> vertex);
  List<Edge<T>> incomingEdges(Vertex<T> vertex);
  Iterable<Vertex<T>> vertices();
  Iterable<Edge<T>> edges();
  boolean removeEdge(long edgeId);
  double setWeight(long edgeId, double weight);
  void initialize(GraphSnapshot<T> snapshot);
  GraphSnapshot<T> snapshot();
}
