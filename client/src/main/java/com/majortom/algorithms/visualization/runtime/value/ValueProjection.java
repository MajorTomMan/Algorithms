package com.majortom.algorithms.visualization.runtime.value;

import java.util.List;

/** Display-only frame projection: neither summary nor fields are algorithm values. */
public record ValueProjection(String summary, List<FieldView> fields, boolean presenterRegistered) {
  public ValueProjection { fields = List.copyOf(fields); }

  public String details() {
    if (!presenterRegistered) return "该类型未声明展示字段";
    StringBuilder text = new StringBuilder();
    for (FieldView field : fields) {
      if (!text.isEmpty()) text.append('\n');
      text.append(field.label()).append("：").append(field.value());
    }
    return text.toString();
  }
}
