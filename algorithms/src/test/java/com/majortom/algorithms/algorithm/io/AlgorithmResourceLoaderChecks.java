package com.majortom.algorithms.algorithm.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.algorithm.graph.model.City;
import java.util.List;
import java.util.Map;

/** Standalone checks for classpath JSON loading with caller-selected types. */
public final class AlgorithmResourceLoaderChecks {
  public static void main(String[] args) {
    City city = AlgorithmResourceLoader.load("io/city.json", City.class);
    check(city.id().equals("a") && city.name().equals("测试甲城"),
        "caller-provided custom class");

    List<City> cities = AlgorithmResourceLoader.load("io/cities.json", new TypeReference<List<City>>() {});
    check(cities.size() == 2 && cities.getFirst().name().equals("测试甲城"),
        "generic list preserves custom element type");

    City[] array = AlgorithmResourceLoader.load("/io/cities.json", City[].class);
    check(array.length == 2 && array[1].id().equals("b"),
        "array class target and leading slash");

    expectInvalid(() -> AlgorithmResourceLoader.load("io/missing.json", City[].class), "resource not found");
    expectInvalid(() -> AlgorithmResourceLoader.load("../graph/regions/demo.json", Object.class),
        "Invalid classpath resource path");
    expectInvalid(() -> AlgorithmResourceLoader.load("io/invalid.json", Object.class),
        "Cannot read JSON resource");
    // CSV rows use the header to bind to the caller's own Java record.
    List<City> csvCities = AlgorithmResourceLoader.loadCsv("io/cities.csv", City.class);
    check(csvCities.size() == 3 && "测试,甲城".equals(csvCities.get(0).name()),
        "CSV header mapping and quoted comma");
    check("测试\"乙城".equals(csvCities.get(1).name()),
        "CSV escaped double quote");
    check("跨行\n城市".equals(csvCities.get(2).name()),
        "CSV embedded newline");

    List<Map<String, String>> csvMaps = AlgorithmResourceLoader.loadCsv(
        "/io/cities.csv", new TypeReference<Map<String, String>>() {});
    check(csvMaps.size() == 3 && "a".equals(csvMaps.getFirst().get("id")),
        "CSV generic row type");

    expectInvalid(() -> AlgorithmResourceLoader.loadCsv("io/missing.csv", City.class),
        "CSV resource not found");
    expectInvalid(() -> AlgorithmResourceLoader.loadCsv("../io/cities.csv", City.class),
        "Invalid classpath resource path");
    expectInvalid(() -> AlgorithmResourceLoader.loadCsv("io/invalid.csv", City.class),
        "Cannot read CSV resource");

    System.out.println("AlgorithmResourceLoaderChecks passed");
  }

  private static void expectInvalid(Runnable action, String expected) {
    try {
      action.run();
    } catch (IllegalArgumentException exception) {
      if (exception.getMessage().contains(expected)) return;
      throw new AssertionError("Unexpected error: " + exception.getMessage(), exception);
    }
    throw new AssertionError("Expected failure: " + expected);
  }

  private static void check(boolean success, String message) {
    if (!success) throw new AssertionError(message);
  }
}
