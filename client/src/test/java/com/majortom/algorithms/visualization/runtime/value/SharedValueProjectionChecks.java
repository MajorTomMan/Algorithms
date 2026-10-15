package com.majortom.algorithms.visualization.runtime.value;

import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueType;
import com.majortom.algorithms.core.metadata.GraphDirection;
import com.majortom.algorithms.core.snapshot.GraphSnapshot;
import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinearStructureViewState;
import com.majortom.algorithms.visualization.runtime.linked.LinkedListViewState;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Headless regression for shared value presentation across structure types. */
public final class SharedValueProjectionChecks {
  private static final AtomicInteger DETAIL_READS = new AtomicInteger();

  @ValueType(immutable = true)
  record Student(
      @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "姓名") String name,
      @ValueField(label = "年龄") int age) {
    @ValueField(label = "备注")
    String detail() {
      DETAIL_READS.incrementAndGet();
      return "不可变学生";
    }
  }

  static final class MutableStudent {
    @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "姓名")
    String name;
    @ValueField(label = "年龄")
    int age;
    MutableStudent(String name, int age) { this.name = name; this.age = age; }
  }

  public static void main(String[] args) {
    Student student = new Student("张三", 20);
    var array = ArrayViewState.source(List.of(student));
    var tree = TreeViewState.Node.general(1L, student, List.of());
    var linked = LinkedListViewState.fromValues(List.of(student));
    var stack = new LinearStructureViewState("stack", List.of(student));
    var queue = new LinearStructureViewState("queue", List.of(student));
    var hash = HashTableViewState.fromSnapshot(new HashTableSnapshot<>(
        4, List.of(new HashTableSnapshot.Entry<>(1, "student", student))));
    var graph = GraphViewState.initial(new GraphSnapshot<>(GraphDirection.UNDIRECTED,
        List.of(new GraphSnapshot.Vertex<>(9L, student)), List.of()));

    List<VisualValue> displayed = List.of(array.values().getFirst(), tree.value(),
        linked.nodes().get(1L).value(), stack.values().getFirst(),
        queue.values().getFirst(), hash.entries().getFirst().value(),
        graph.nodes().getFirst().value());

    for (VisualValue value : displayed) {
      check("张三".equals(value.text()), "same summary across all structures");
      check(value.projection().fields().isEmpty(), "immutable detail must remain deferred");
    }
    check(DETAIL_READS.get() == 0, "do not run DETAIL getters while creating frames");
    for (VisualValue value : displayed) {
      String details = value.additionalDetails();
      check(details.contains("姓名：张三") && details.contains("年龄：20")
          && details.contains("备注：不可变学生"), "shared declared detail fields");
    }
    check(DETAIL_READS.get() == displayed.size(), "read details only on selection");

    // Scalar values already occupy the structure's Value row; do not add "值：...".
    check(VisualValue.compact(123).additionalDetails().isEmpty(), "scalar duplicate");
    check(VisualValue.compact("深圳").additionalDetails().isEmpty(), "string duplicate");
    check(hash.entries().getFirst().key().additionalDetails().isEmpty(),
        "scalar hash key duplicate");

    MutableStudent changing = new MutableStudent("李四", 20);
    VisualValue oldFrame = ArrayViewState.source(List.of(changing)).values().getFirst();
    check(oldFrame.projection().fieldsDeclared(), "mutable details captured eagerly");
    changing.age = 21;
    check(oldFrame.additionalDetails().contains("年龄：20"), "historical frame must not drift");
    check("李四".equals(oldFrame.text()), "historical summary captured");
    VisualValue nextFrame = ArrayViewState.source(List.of(changing)).values().getFirst();
    check(nextFrame.additionalDetails().contains("年龄：21"), "new frame reflects mutation");

    System.out.println("SharedValueProjectionChecks passed");
  }

  private static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
