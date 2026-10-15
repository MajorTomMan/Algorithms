package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueType;

/** Annotation-only value projection checks; no UI toolkit required. */
public final class AnnotatedValueProjectionChecks {
  @ValueType(name = "城市")
  record City(
      @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL},
          label = "名称") String name,
      @ValueField(label = "人口") int population) {
    @ValueField(value = ValueDisplay.DETAIL, label = "分类")
    String category() { return "地级市"; }
  }

  static final class Address {
    @Override public String toString() {
      throw new AssertionError("Presentation must not call nested object toString()");
    }
  }

  record District(
      @ValueField(value = ValueDisplay.SUMMARY, label = "名称") String name,
      @ValueField(label = "地址") Address address) {}

  static final class LazyCity {
    private int detailReads;
    @ValueField(ValueDisplay.SUMMARY)
    String name() { return "深圳"; }
    @ValueField(ValueDisplay.DETAIL)
    String details() { detailReads++; return "业务详情"; }
  }

  record Opaque(String name) {}

  public static void main(String[] args) {
    City city = new City("深圳", 18000000);
    var projection = ValueAdapters.project(city);
    check(projection.summary().equals("深圳"), "annotated summary");
    check(projection.fieldsDeclared() && projection.details().contains("名称：深圳")
        && projection.details().contains("人口：18000000")
        && projection.details().contains("分类：地级市"), "annotated details");

    // Custom input parsing must not override the annotation-based display model.
    ValueAdapters.register(new ValueAdapter<City>() {
      @Override public Class<City> type() { return City.class; }
      @Override public City parse(String text) { return new City(text, 1); }
      @Override public String format(City value) { return "INPUT:" + value.name(); }
    });
    check(ValueAdapters.require(City.class).parse("广州").name().equals("广州"),
        "custom input parsing retained");
    check(ValueAdapters.summary(city).equals("深圳")
        && ValueAdapters.project(city).summary().equals("深圳"),
        "display must remain annotation-driven");

    LazyCity lazy = new LazyCity();
    check(ValueAdapters.summary(lazy).equals("深圳") && lazy.detailReads == 0,
        "summary must not invoke DETAIL getter");
    check(ValueAdapters.project(lazy).details().contains("业务详情")
        && lazy.detailReads == 1, "details only evaluated upon inspection");

    District district = new District("南山", new Address());
    check(ValueAdapters.summary(district).equals("南山"), "summary only fields");
    check(ValueAdapters.project(district).details().contains("地址：Address"),
        "nested objects are not recursively expanded or stringified");

    check(ValueAdapters.summary(new Opaque("secret")).equals("Opaque")
        && !ValueAdapters.project(new Opaque("secret")).fieldsDeclared(),
        "unannotated object falls back to type name, never toString()");
    for (Object value : new Object[] { 7, "深圳", true, 'A', 1.5 }) {
      var field = ValueAdapters.project(value);
      check(field.fieldsDeclared() && field.fields().size() == 1
          && "value".equals(field.fields().getFirst().key())
          && field.summary().equals(String.valueOf(value))
          && field.details().contains("值："), "built-in scalar projection");
    }
    check(ValueAdapters.project(null).summary().equals("null"),
        "null projection retained");
    check(ValueAdapters.require(Integer.class).parse("12") == 12
        && ValueAdapters.distinctValue(Integer.class, 3).equals(3),
        "built-in editing and data generation retained");
    System.out.println("AnnotatedValueProjectionChecks passed");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
