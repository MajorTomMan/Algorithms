package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueType;

/** Manual annotation presentation check, no UI toolkit required. */
public final class AnnotatedValueProjectionChecks {
  @ValueType(name = "城市")
  record City(
      @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL},
          label = "名称") String name,
      @ValueField(label = "人口") int population) {
    @ValueField(value = ValueDisplay.DETAIL, label = "分类")
    String category() { return "地级市"; }
  }
  public static void main(String[] args) {
    var projection = ValueAdapters.project(new City("深圳", 18000000));
    if (!projection.summary().equals("深圳")) throw new AssertionError("summary");
    if (!projection.details().contains("名称：深圳")
        || !projection.details().contains("人口：18000000")
        || !projection.details().contains("分类：地级市"))
      throw new AssertionError("detail fields: " + projection.details());
    System.out.println("AnnotatedValueProjectionChecks passed");
  }
}
