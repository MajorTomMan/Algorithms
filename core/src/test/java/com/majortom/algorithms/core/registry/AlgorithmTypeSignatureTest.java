package com.majortom.algorithms.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlgorithmTypeSignatureTest {

  @Test
  void preservesOrderAndArity() {
    AlgorithmTypeSignature signature =
        AlgorithmTypeSignature.of(String.class, Integer.class);

    assertEquals(2, signature.arity());
    assertEquals(String.class, signature.type(0));
    assertEquals(Integer.class, signature.type(1));
    assertEquals(List.of(String.class, Integer.class), signature.types());
  }

  @Test
  void rejectsEmptySignature() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AlgorithmTypeSignature.of());
  }
}
