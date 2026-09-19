package com.majortom.algorithms.visualization.runtime.value;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValuePresentationExtensionTest {
  record Person(String name, int age, String address) {}
  record Undeclared(int value) {}
  record Exploding(int id) {}

  @Test
  void manuallyEnteredCustomTypeKeepsItsClassAndUsesOnlyDeclaredFields() {
    ValueAdapters.register(new ValueAdapter<Person>() {
      @Override public Class<Person> type() { return Person.class; }
      @Override public Person parse(String text) {
        String[] segments = text.split(":", 3);
        if (segments.length != 3) throw new IllegalArgumentException("Expected name:age:address");
        return new Person(segments[0], Integer.parseInt(segments[1]), segments[2]);
      }
      @Override public String format(Person value) {
        return value.name() + ":" + value.age() + ":" + value.address();
      }
    });
    ValueAdapters.registerPresenter(new ValuePresenter<Person>() {
      @Override public Class<Person> type() { return Person.class; }
      @Override public List<DisplayField<Person>> fields() { return List.of(
          new DisplayField<>("name", "姓名", Person::name, true),
          new DisplayField<>("age", "年龄", Person::age, true),
          new DisplayField<>("address", "地址", Person::address, false)); }
    });
    assertFalse(ValueAdapters.canReplay(Person.class), "custom value must explicitly declare immutability");
    ValueAdapters.declareImmutable(Person.class);
    assertTrue(ValueAdapters.canReplay(Person.class));
    Person value = ValueAdapters.require(Person.class).parse("李四:25:北京市朝阳区");
    assertEquals(Person.class, ValueAdapters.requireType(Person.class.getName()));
    assertEquals(value, ValueAdapters.require(Person.class).parse(ValueAdapters.require(Person.class).format(value)));
    assertFalse(ValueAdapters.canGenerate(Person.class));
    assertEquals(0, ValueAdapters.maxDistinctSamples(Person.class));
    assertThrows(IllegalArgumentException.class, () -> ValueAdapters.randomValue(Person.class, new java.util.Random()));
    assertEquals("李四 · 25", VisualValue.of(value).text());
    assertEquals("姓名：李四\n年龄：25\n地址：北京市朝阳区", ValueAdapters.project(value).details());
    assertEquals(value, VisualValue.of(value).value());
  }

  @Test
  void getterFailuresAndUndeclaredFieldsDoNotBreakProjection() {
    ValueAdapters.register(new ValueAdapter<Exploding>() {
      @Override public Class<Exploding> type() { return Exploding.class; }
      @Override public Exploding parse(String value) { return new Exploding(Integer.parseInt(value)); }
      @Override public String format(Exploding value) { return Integer.toString(value.id()); }
    }, null, null, new ValuePresenter<Exploding>() {
      @Override public Class<Exploding> type() { return Exploding.class; }
      @Override public List<DisplayField<Exploding>> fields() { return List.of(
          new DisplayField<>("bad", "异常字段", p -> { throw new IllegalStateException("boom"); }, true),
          new DisplayField<>("good", "正常字段", Exploding::id, true),
          new DisplayField<>("nullable", "可空字段", p -> null, false)); }
    });
    assertEquals("<error> · 3", ValueAdapters.project(new Exploding(3)).summary());
    assertEquals("异常字段：<error>\n正常字段：3\n可空字段：null",
        ValueAdapters.project(new Exploding(3)).details());
    ValueAdapters.register(new ValueAdapter<Undeclared>() {
      @Override public Class<Undeclared> type() { return Undeclared.class; }
      @Override public Undeclared parse(String text) { return new Undeclared(Integer.parseInt(text)); }
      @Override public String format(Undeclared value) { return Integer.toString(value.value()); }
    });
    assertEquals("该类型未声明展示字段", ValueAdapters.project(new Undeclared(5)).details());
  }

  @Test
  void duplicateKeysAndUnsupportedScalarInputAreRejected() {
    record Duplicate(int id) {}
    ValuePresenter<Duplicate> bad = new ValuePresenter<>() {
      @Override public Class<Duplicate> type() { return Duplicate.class; }
      @Override public List<DisplayField<Duplicate>> fields() { return List.of(
          new DisplayField<>("id", "编号", Duplicate::id, true),
          new DisplayField<>("id", "重复编号", Duplicate::id, false)); }
    };
    assertThrows(IllegalArgumentException.class, () -> ValueAdapters.registerPresenter(bad));
    assertThrows(NumberFormatException.class, () -> ValueAdapters.require(Double.class).parse("NaN"));
  }
}
