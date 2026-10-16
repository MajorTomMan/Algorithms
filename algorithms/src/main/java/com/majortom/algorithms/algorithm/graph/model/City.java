package com.majortom.algorithms.algorithm.graph.model;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import com.majortom.algorithms.structure.graph.GraphStructure;

/** A city is identified by its stable business ID, not the graph vertex ID. */
@ValueType(value = GraphStructure.class, name = "城市", immutable = true)
/**
 * 图路网的业务顶点值：{@code id} 是稳定城市 ID，{@code name} 是展示名称。
 *
 * <p>@ValueType(name="城市") 让客户端发现此类型并在图模块选择；
 * @ValueId 供 GraphObjectMapper 将道路端点实体与 Region.cities 关联。
 * 这不是 Vertex 的 long 内部 ID，两者用途不同。
 *
 * <p>record 的字段均为不可变 String，因此可以声明 immutable=true；
 * @ValueField 决定用户看到的摘要与详情，不影响 JSON 结构及拓扑映射。
 */
public record City(
    @ValueId
    @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "城市编号")
    String id,
    @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "城市名称")
    String name) {

  public City {
    id = requireText(id, "city id");
    name = requireText(name, "city name");
  }

  private static String requireText(String text, String field) {
    if (text == null || text.isBlank())
      throw new IllegalArgumentException(field + " cannot be blank");
    return text.trim();
  }
}
