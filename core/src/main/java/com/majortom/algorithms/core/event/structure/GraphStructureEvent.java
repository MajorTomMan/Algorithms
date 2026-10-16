package com.majortom.algorithms.core.event.structure;

import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import java.util.List;

/**
 * Graph 结构状态变化的事实事件集合，支持多重图和平行边。
 *
 * <p>每条边由 edgeId 唯一标识，端点相同也不会合并。
 * 增删和权重变化作为结构真值事件进入 ExecutionEvents.structure；
 * GraphEventReducer 消费后重建 GraphViewState。
 *
 * <p>Reset(GraphSnapshot) 表示一次原子整图替换：
 * Graph.initialize 校验成功后发送单条 Reset，
 * 不通过零散的 Added 事件展示半初始化状态。
 */
public sealed interface GraphStructureEvent extends StructureEvent
    permits GraphStructureEvent.VertexAdded, GraphStructureEvent.VertexRemoved,
            GraphStructureEvent.EdgeAdded, GraphStructureEvent.EdgesAdded,
            GraphStructureEvent.EdgeRemoved, GraphStructureEvent.EdgeWeightChanged,
            GraphStructureEvent.Reset {
  record VertexAdded(long vertexId, Object value) implements GraphStructureEvent {}
  record VertexRemoved(long vertexId, Object value, List<Long> incidentEdges)
      implements GraphStructureEvent {
    public VertexRemoved(long vertexId, Object value) {
      this(vertexId, value, List.of());
    }
    public VertexRemoved { incidentEdges = List.copyOf(incidentEdges); }
  }
  record EdgeAdded(long edgeId, long fromId, long toId, Object value, double weight)
      implements GraphStructureEvent {
    public EdgeAdded(long edgeId, long fromId, long toId, double weight) {
      this(edgeId, fromId, toId, null, weight);
    }
  }
  record EdgesAdded(EdgeAdded forward, EdgeAdded reverse) implements GraphStructureEvent {}
  record EdgeRemoved(long edgeId, long fromId, long toId) implements GraphStructureEvent {
    public EdgeRemoved(long edgeId) { this(edgeId, 0L, 0L); }
  }
  record EdgeWeightChanged(long edgeId, double previousWeight, double weight)
      implements GraphStructureEvent {
    public double previous() { return previousWeight; }
    public double current() { return weight; }
  }
  record Reset(GraphSnapshot<?> snapshot) implements GraphStructureEvent {}
}
