package com.majortom.algorithms.visualization.runtime.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ValueAdaptersTest {
  @Test
  void parsesAndFormatsEverySupportedScalarType() {
    assertEquals(List.of(Integer.class, String.class, Byte.class, Short.class, Long.class,
        Float.class, Double.class, Boolean.class, Character.class),
        ValueAdapters.supportedTypes().subList(0, 9));
    assertEquals(Byte.valueOf((byte) 12), ValueAdapters.require(Byte.class).parse(" 12 "));
    assertEquals(Short.valueOf((short) 123), ValueAdapters.require(Short.class).parse("123"));
    assertEquals(123L, ValueAdapters.require(Long.class).parse("123"));
    assertEquals(1.5f, ValueAdapters.require(Float.class).parse("1.5"));
    assertEquals(1.5d, ValueAdapters.require(Double.class).parse("1.5"));
    assertEquals(true, ValueAdapters.require(Boolean.class).parse("TRUE"));
    assertEquals(false, ValueAdapters.require(Boolean.class).parse("false"));
    assertEquals('A', ValueAdapters.require(Character.class).parse(" A "));
    assertEquals("hello", ValueAdapters.require(String.class).parse(" hello "));
    for (Class<?> type : ValueAdapters.supportedTypes().subList(0, 9)) {
      assertEquals(type, ValueAdapters.requireType(type.getSimpleName()));
      assertTrue(ValueAdapters.supports(type));
      assertEquals(type, ValueAdapters.randomValue(type, new Random(42)).getClass());
      assertEquals(type, ValueAdapters.distinctValue(type, 0).getClass());
    }
    assertFalse(ValueAdapters.supports(Object.class));
  }

  @Test
  void rejectsInvalidInputAndRespectsSmallDistinctDomains() {
    assertThrows(IllegalArgumentException.class, () -> ValueAdapters.require(Boolean.class).parse("maybe"));
    assertThrows(IllegalArgumentException.class, () -> ValueAdapters.require(Character.class).parse("ab"));
    assertThrows(NumberFormatException.class, () -> ValueAdapters.require(Byte.class).parse("128"));
    assertThrows(NumberFormatException.class, () -> ValueAdapters.require(Float.class).parse("NaN"));
    assertThrows(NumberFormatException.class, () -> ValueAdapters.require(Double.class).parse("Infinity"));
    assertEquals(2, ValueAdapters.maxDistinctSamples(Boolean.class));
    assertEquals(26, ValueAdapters.maxDistinctSamples(Character.class));
    assertEquals(false, ValueAdapters.distinctValue(Boolean.class, 0));
    assertEquals(true, ValueAdapters.distinctValue(Boolean.class, 1));
    assertThrows(IllegalArgumentException.class, () -> ValueAdapters.distinctValue(Boolean.class, 2));
  }

  @Test
  void annotationTypeSelectsMatchingAlgorithmEvenWhenIdIsShared() {
    assertEquals(Integer.class,
        AlgorithmCatalog.descriptor("array", Integer.class, "insertion-sort").valueType());
    assertEquals(String.class,
        AlgorithmCatalog.descriptor("array", String.class, "insertion-sort").valueType());
    assertTrue(AlgorithmCatalog.forWorkbenchModule("array", String.class).contains("insertion-sort"));
    assertTrue(AlgorithmCatalog.forWorkbenchModule("stack", Integer.class).isEmpty());
    assertEquals(String.class,
        AlgorithmCatalog.descriptor("stack", String.class, "reverse-polish-expression").valueType());
  }
}
