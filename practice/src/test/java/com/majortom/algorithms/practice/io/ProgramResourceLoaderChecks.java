package com.majortom.algorithms.practice.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

/** Standalone checks for the program/practice resource loader. */
public final class ProgramResourceLoaderChecks {
  public record Case(String id, List<Integer> numbers, int expected) {}
  public record CaseSet(String name, List<Case> cases) {}
  public record CsvCase(String id, int left, int right, int expected) {}

  public static void main(String[] args) {
    CaseSet data = ProgramResourceLoader.load("program/demo.json", CaseSet.class);
    check("示例程序输入".equals(data.name())
        && data.cases().size() == 2
        && data.cases().getFirst().numbers().stream().mapToInt(Integer::intValue).sum() == 6,
        "custom record including nested data");

    Map<String, Object> fields = ProgramResourceLoader.load("program/demo.json",
        new TypeReference<Map<String, Object>>() {});
    check(fields.containsKey("cases") && fields.containsKey("name"),
        "parameterized map target");

    boolean rejected = false;
    try {
      ProgramResourceLoader.load("program/missing.json", CaseSet.class);
    } catch (IllegalArgumentException expected) {
      rejected = expected.getMessage().contains("resource not found");
    }
    check(rejected, "missing program resource has a readable error");
    List<CsvCase> csvCases = ProgramResourceLoader.loadCsv("program/cases.csv", CsvCase.class);
    check(csvCases.size() == 2
        && csvCases.getFirst().left() + csvCases.getFirst().right()
            == csvCases.getFirst().expected()
        && csvCases.getLast().expected() == 21,
        "CSV records with numeric properties");

    List<Map<String, String>> csvRows = ProgramResourceLoader.loadCsv(
        "program/cases.csv", new TypeReference<Map<String, String>>() {});
    check(csvRows.size() == 2 && "21".equals(csvRows.getLast().get("expected")),
        "CSV generic map rows");

    System.out.println("ProgramResourceLoaderChecks passed");
  }

  private static void check(boolean success, String message) {
    if (!success) throw new AssertionError(message);
  }
}
