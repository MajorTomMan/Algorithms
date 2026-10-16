package com.majortom.algorithms.algorithm.graph.model;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import com.majortom.algorithms.structure.graph.GraphStructure;

/** A city is identified by its stable business ID, not the graph vertex ID. */
@ValueType(value = GraphStructure.class, name = "城市", immutable = true)
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
