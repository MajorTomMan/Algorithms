package com.majortom.algorithms.algorithm.graph.model;

import java.util.Objects;

/**
 * 业务道路实体，作为实际 Edge&lt;City&gt; 的 payload 保留。
 *
 * <p>端点是 City 对象，而不是字符串 ID；因此 roads.json 中 from/to
 * 若存放字符串，不能直接 Jackson 反序列化为 Road。
 * 当前示例 JSON 将 City 作为嵌套对象，GraphObjectMapper 再借助
 * @ValueId 以业务 ID 关联 Region.cities 的顶点。
 *
 * <p>bidirectional 的道路在有向图中映射为两个不同边 ID，
 * 在无向图中一条边即可双向访问；道路距离必须是非负有限值。
 * 这里不负责构图、最短路径计算或 JavaFX 事件回放。
 */
public record Road(String id, City from, City to, double distance, boolean bidirectional) {
  public Road {
    if (id == null || id.isBlank())
      throw new IllegalArgumentException("road id cannot be blank");
    id = id.trim();
    Objects.requireNonNull(from, "road from");
    Objects.requireNonNull(to, "road to");
    if (!Double.isFinite(distance) || distance < 0)
      throw new IllegalArgumentException("road distance must be finite and non-negative");
    if (bidirectional && from.id().equals(to.id()))
      throw new IllegalArgumentException("bidirectional self-loop is ambiguous");
  }
}
