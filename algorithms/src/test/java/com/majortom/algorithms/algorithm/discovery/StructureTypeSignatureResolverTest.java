package com.majortom.algorithms.algorithm.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.majortom.algorithms.structure.hash.HashTableStructure;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.RegistrationException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureTypeSignatureResolverTest {

  @Test
  void preservesOrderedHashKeyAndValueTypes() throws Exception {
    Method method = Fixture.class.getDeclaredMethod("run", HashTableStructure.class);

    List<Class<?>> types = StructureTypeSignatureResolver.resolve(
        method.getGenericParameterTypes()[0],
        HashTableStructure.class);

    assertEquals(List.of(String.class, Integer.class), types);
  }

  @Test
  void rejectsSwappedHashKeyAndValueTypes() throws Exception {
    Method method = Fixture.class.getDeclaredMethod("run", HashTableStructure.class);
    AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
        "hash-test",
        "Hash Test",
        AlgorithmTypeSignature.of(Integer.class, String.class),
        HashTableStructure.class,
        Fixture.class,
        method);

    assertThrows(
        RegistrationException.class,
        () -> new AlgorithmDiscovery().validateTypeSignature(descriptor));
  }

  private static final class Fixture {
    @SuppressWarnings("unused")
    public void run(HashTableStructure<String, Integer> table) {}
  }
}
