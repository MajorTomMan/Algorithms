package com.majortom.algorithms.practice.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

/** Standalone checks for the program/practice resource loader. */
public final class ProgramResourceLoaderChecks {
  public record Case(String id, List<Integer> numbers, int expected) {}
  public record CaseSet(String name, List<Case> cases) {}

  public static void main(String[] args) {
    var loader = new ProgramResourceLoader();
    CaseSet data = loader.load("program/demo.json", CaseSet.class);
    check("示例程序输入".equals(data.name())
        && data.cases().size() == 2
        && data.cases().getFirst().numbers().stream().mapToInt(Integer::intValue).sum() == 6,
        "custom record including nested data");

    Map<String, Object> fields = loader.load("program/demo.json",
        new TypeReference<Map<String, Object>>() {});
    check(fields.containsKey("cases") && fields.containsKey("name"),
        "parameterized map target");

    boolean rejected = false;
    try {
      loader.load("program/missing.json", CaseSet.class);
    } catch (IllegalArgumentException expected) {
      rejected = expected.getMessage().contains("resource not found");
    }
    check(rejected, "missing program resource has a readable error");
    System.out.println("ProgramResourceLoaderChecks passed");
  }

  private static void check(boolean success, String message) {
    if (!success) throw new AssertionError(message);
  }
}
