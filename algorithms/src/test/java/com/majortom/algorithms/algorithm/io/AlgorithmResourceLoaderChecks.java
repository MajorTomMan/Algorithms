package com.majortom.algorithms.algorithm.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.algorithm.graph.model.City;
import java.util.List;

/** Standalone checks for classpath JSON loading with caller-selected types. */
public final class AlgorithmResourceLoaderChecks {
  public static void main(String[] args) {
    var loader = new AlgorithmResourceLoader();

    City city = loader.load("io/city.json", City.class);
    check(city.id().equals("a") && city.name().equals("测试甲城"),
        "caller-provided custom class");

    List<City> cities = loader.load("io/cities.json", new TypeReference<List<City>>() {});
    check(cities.size() == 2 && cities.getFirst().name().equals("测试甲城"),
        "generic list preserves custom element type");

    City[] array = loader.load("/io/cities.json", City[].class);
    check(array.length == 2 && array[1].id().equals("b"),
        "array class target and leading slash");

    expectInvalid(() -> loader.load("io/missing.json", City[].class), "resource not found");
    expectInvalid(() -> loader.load("../graph/regions/demo.json", Object.class),
        "Invalid classpath resource path");
    expectInvalid(() -> loader.load("io/invalid.json", Object.class),
        "Cannot read JSON resource");
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
