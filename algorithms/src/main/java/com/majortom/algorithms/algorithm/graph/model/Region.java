package com.majortom.algorithms.algorithm.graph.model;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import java.util.List;
import java.util.Objects;

/**
 * 路网业务聚合根，同时声明 Graph.from(...) 如何读取拓扑。
 *
 * <p>@GraphNodes 指向 City 顶点集合；@GraphEdges 指向 Road 边集合，
 * 并告诉元数据层如何从每条 Road 读取起点、终点、距离和双向标志。
 * 本对象本身不是 Graph，也不保存内部 Vertex/Edge ID。
 *
 * <p>数据准备阶段将 JSON 读取为 City、Road 后创建 Region；
 * GraphObjectMapper 先收集城市并索引 @ValueId，再解析道路，
 * Graph.fromSnapshot 最终创建领域图。这样业务数据与图实现保持解耦。
 */
public record Region(
    String name,
    @GraphNodes List<City> cities,
    @GraphEdges(from = "from", to = "to",
        weight = "distance", bidirectional = "bidirectional")
    List<Road> roads) {

  public Region {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("region name cannot be blank");
    name = name.trim();
    cities = List.copyOf(Objects.requireNonNull(cities, "cities"));
    roads = List.copyOf(Objects.requireNonNull(roads, "roads"));
  }
}
