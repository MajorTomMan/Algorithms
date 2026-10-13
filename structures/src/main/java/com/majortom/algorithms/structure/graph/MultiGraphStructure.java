package com.majortom.algorithms.structure.graph;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.snapshot.MultiGraphSnapshot;
import java.util.List;
@Structure(id = StructureIds.MULTI_GRAPH, name = "MultiGraph",
    module = StructureModule.GRAPH, implementation = MultiGraph.class)
public interface MultiGraphStructure<T> {
  GraphDirection direction();
  int vertexCount();
  int edgeCount();
  Vertex<T> addVertex(T value);
  Vertex<T> vertex(long id);
  boolean removeVertex(long id);
  MultiEdge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value, double weight);
  default MultiEdge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value) {
    return addEdge(from, to, value, 1.0);
  }
  default MultiEdge<T> addEdge(Vertex<T> from, Vertex<T> to) {
    return addEdge(from, to, null, 1.0);
  }
  MultiGraph.BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to,
                                 Object forwardValue, Object reverseValue,
                                 double forwardWeight, double reverseWeight);
  default MultiGraph.BiEdges<T> addBiEdge(Vertex<T> from, Vertex<T> to, Object value) {
    return addBiEdge(from, to, value, value, 1.0, 1.0);
  }
  MultiEdge<T> edge(long id);
  List<MultiEdge<T>> edges(Vertex<T> from, Vertex<T> to);
  List<MultiEdge<T>> outgoingEdges(Vertex<T> vertex);
  List<MultiEdge<T>> incomingEdges(Vertex<T> vertex);
  Iterable<Vertex<T>> vertices();
  Iterable<MultiEdge<T>> edges();
  boolean removeEdge(long edgeId);
  double setWeight(long edgeId, double weight);
  void initialize(MultiGraphSnapshot<T> snapshot);
  MultiGraphSnapshot<T> snapshot();
}
