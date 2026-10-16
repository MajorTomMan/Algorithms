package com.majortom.algorithms.core.snapshot;
import com.majortom.algorithms.core.metadata.GraphDirection;
import java.util.List;
import java.util.Objects;
/**
 * 不包含 JavaFX 对象的图拓扑快照，供结构、运行记录与 UI 之间传输。
 *
 * <p>保存方向、顶点 ID、边 ID、权重及可选 payload；复制列表以固定拓扑，
 * 但不会自动深拷贝节点值或边 payload。可靠回放要求值本身不可变，
 * 或在进入事件流之前显式冻结。
 *
 * <p>Graph.fromSnapshot 创建新图，Graph.initialize 校验后替换已有图并发送 Reset；
 * 业务 @ValueId 与内部顶点 long ID 不是同一个标识。
 */
public record GraphSnapshot<T>(GraphDirection direction, List<Vertex<T>> vertices,
                                    List<Edge> edges) {
  public GraphSnapshot {
    direction = Objects.requireNonNull(direction, "direction");
    vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
    edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
  }
  public record Vertex<T>(long id, T value) {
    public Vertex {
      if (id <= 0) throw new IllegalArgumentException("vertex ID must be positive");
      Objects.requireNonNull(value, "value");
    }
  }
  public record Edge(long id, long fromId, long toId, Object value, double weight) {
    public Edge(long id, long fromId, long toId, double weight) {
      this(id, fromId, toId, null, weight);
    }
    public Edge {
      if (id <= 0 || fromId <= 0 || toId <= 0 || !Double.isFinite(weight))
        throw new IllegalArgumentException("invalid multi-graph edge");
    }
  }
}
