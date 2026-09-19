package com.majortom.algorithms.visualization.runtime.value;

import static org.junit.jupiter.api.Assertions.*;

import com.majortom.algorithms.visualization.runtime.VisualValue;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BatchInputParserTest {
  record Person(String name, int age, String address) {}
  static final class ChangingValue {
    String label = "original";
  }

  @Test
  void customBatchKeepsWhitespaceAndCommaInsideAnElement() {
    ValueAdapter<Person> adapter = new ValueAdapter<>() {
      @Override public Class<Person> type() { return Person.class; }
      @Override public Person parse(String text) {
        String[] fields = text.split(":", 3);
        return new Person(fields[0], Integer.parseInt(fields[1]), fields[2]);
      }
      @Override public String format(Person value) { return value.name() + ":" + value.age() + ":" + value.address(); }
    };
    List<Person> rows = BatchInputParser.parse("李四:25:北京, 朝阳区;王五:30:上海 浦东", adapter);
    assertEquals(2, rows.size());
    assertEquals("北京, 朝阳区", rows.getFirst().address());
    assertEquals("上海 浦东", rows.get(1).address());
    assertEquals(List.of(1, 2, 3), BatchInputParser.parse("1, 2;3", ValueAdapters.require(Integer.class)));
    assertThrows(IllegalArgumentException.class, () -> BatchInputParser.parse("李四:25:北京;王五:BAD:上海", adapter));
  }

  @Test
  void oneFrameCapturesSummaryAndFieldsOnlyOnce() {
    AtomicInteger calls = new AtomicInteger();
    ValueAdapters.register(new ValueAdapter<ChangingValue>() {
      @Override public Class<ChangingValue> type() { return ChangingValue.class; }
      @Override public ChangingValue parse(String text) { ChangingValue value = new ChangingValue(); value.label = text; return value; }
      @Override public String format(ChangingValue value) { return value.label; }
    }, null, null, new ValuePresenter<ChangingValue>() {
      @Override public Class<ChangingValue> type() { return ChangingValue.class; }
      @Override public List<DisplayField<ChangingValue>> fields() {
        return List.of(new DisplayField<>("label", "标签", value -> { calls.incrementAndGet(); return value.label; }, true));
      }
    });
    assertFalse(ValueAdapters.canReplay(ChangingValue.class), "mutable values cannot enter history/replay");
    assertTrue(ValueAdapters.canReplay(Integer.class), "builtin scalar values are immutable");
    ChangingValue mutable = new ChangingValue();
    VisualValue frame = VisualValue.of(mutable);
    assertEquals("original", frame.text());
    assertEquals("标签：original", frame.projection().details());
    assertEquals(1, calls.get());
    mutable.label = "updated";
    assertEquals("标签：original", frame.projection().details(), "Inspector must use the frame's evaluated fields");
    assertEquals("updated", VisualValue.of(mutable).text(), "Next frame can reflect the new value");
    assertEquals(2, calls.get());
    assertSame(mutable, frame.value(), "Algorithm value remains the original object");
  }
}
