package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.ComponentNames;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AlgorithmDiscovery {
  public static final String ROOT_PACKAGE = "com.majortom.algorithms.algorithm";

  private final FrameworkClassScanner scanner;

  public AlgorithmDiscovery() {
    this(new FrameworkClassScanner());
  }

  AlgorithmDiscovery(FrameworkClassScanner scanner) {
    this.scanner = Objects.requireNonNull(scanner, "scanner");
  }

  public List<AlgorithmDescriptor> discover(ClassLoader classLoader) {
    Objects.requireNonNull(classLoader, "classLoader");
    List<AlgorithmDescriptor> descriptors = new ArrayList<>();
    for (Class<?> implementation : scanner.scan(ROOT_PACKAGE, classLoader)) {
      Algorithm annotation = implementation.getAnnotation(Algorithm.class);
      if (annotation == null) {
        continue;
      }

      Class<?> structureContract = annotation.structure();
      if (structureContract.getAnnotation(Structure.class) == null) {
        throw new RegistrationException(
            "Algorithm structure contract is missing @Structure metadata: "
                + structureContract.getName());
      }

      Method entryPoint = findEntryPoint(implementation);
      AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
          annotation.id(),
          ComponentNames.resolve(annotation.name(), implementation),
          AlgorithmTypeSignature.of(annotation.types()),
          structureContract,
          implementation,
          entryPoint);
      RegistrationValidator.validate(descriptor);
      validateTypeSignature(descriptor);
      descriptors.add(descriptor);
    }

    descriptors.sort(
        Comparator.comparing((AlgorithmDescriptor descriptor) -> descriptor.module().id())
            .thenComparing(descriptor -> descriptor.structureContract().getName())
            .thenComparing(descriptor -> descriptor.typeSignature().stableName())
            .thenComparing(AlgorithmDescriptor::id));
    RegistrationValidator.validateUniqueAlgorithmKeys(descriptors);
    return List.copyOf(descriptors);
  }

  void validateTypeSignature(AlgorithmDescriptor descriptor) {
    if (descriptor.structureContract().getTypeParameters().length == 0) {
      return;
    }

    List<Class<?>> entryTypes = StructureTypeSignatureResolver.resolve(
        descriptor.entryPoint().getGenericParameterTypes()[0],
        descriptor.structureContract());
    if (!entryTypes.equals(descriptor.typeSignature().types())) {
      throw new RegistrationException(
          "Algorithm annotation types " + descriptor.typeSignature().types()
              + " do not match ordered entry Structure types " + entryTypes
              + " for " + descriptor.implementation().getName());
    }
  }

  private Method findEntryPoint(Class<?> implementation) {
    List<Method> entries = new ArrayList<>();
    for (Method method : implementation.getMethods()) {
      if (method.getAnnotation(AlgorithmEntry.class) != null) {
        entries.add(method);
      }
    }
    if (entries.size() != 1) {
      throw new RegistrationException("Algorithm " + implementation.getName()
          + " must expose exactly one @AlgorithmEntry method, found " + entries.size());
    }
    return entries.getFirst();
  }
}
