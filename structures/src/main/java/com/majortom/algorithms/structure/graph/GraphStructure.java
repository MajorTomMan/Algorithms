package com.majortom.algorithms.structure.graph;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.List;
@Structure(id = StructureIds.GRAPH, name = "Graph",
    module = StructureModule.GRAPH, implementation = Graph.class)
/**
 * 图算法面向的能力契约：支持方向、具独立 ID 的顶点/边及快照初始化。
 *
 * <p>算法依赖此接口，不直接依赖实现类 Graph、文件加载器或可视化。
 * 同值顶点允许并存；平行边和自环是合法拓扑。所有边查询以真实 Vertex
 * 身份或边 ID 为依据，不能简单按业务值去重。
 *
 * <p>{@link #snapshot()} 是跨层状态边界；{@link #initialize(GraphSnapshot)}
 * 用快照替换结构而不是执行单个增删操作。实现应先校验新状态，
 * 再发布一个 Reset 事实，防止可视化看到半成品。
 */
public interface GraphStructure<T> {
  GraphDirection direction();
  default boolean isDirected() { return direction().isDirected(); }
  default boolean isEmpty() { return vertexCount() == 0; }
  int vertexCount();
  int edgeCount();
  Vertex<T> addVertex(T value);
  boolean containsVertex(Vertex<T> vertex);
  boolean containsEdge(Vertex<T> from, Vertex<T> to);
  Vertex<T> vertex(long id);
  boolean removeVertex(long id);
  boolean removeVertex(Vertex<T> vertex);
  Edge<T> addEdge(Vertex<T> from, Vertex<T> to, Object value, double weight);
  Edge<T> addEdge(Vertex<T> from, Vertex<T> to, double weight);
  double weight(Edge<T> edge);
  double setWeight(Edge<T> edge, double weight);
  Iterable<Vertex<T>> neighbors(Vertex<T> vertex);
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
