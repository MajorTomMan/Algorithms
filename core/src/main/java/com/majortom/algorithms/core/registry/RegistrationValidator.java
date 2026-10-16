package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.annotation.AlgorithmInit;
import com.majortom.algorithms.core.annotation.Structure;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class RegistrationValidator {
  private static final Pattern COMPONENT_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

  private RegistrationValidator() {}

  public static StructureDescriptor validate(StructureDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    validateId(descriptor.id(), "Structure");
    validateContract(descriptor.contract(), "Structure");
    validateImplementation(descriptor.implementation(), "Structure");
    if (!descriptor.contract().isAssignableFrom(descriptor.implementation())) {
      throw new RegistrationException("Structure implementation "
          + descriptor.implementation().getName() + " does not implement declared contract "
          + descriptor.contract().getName());
    }
    return descriptor;
  }

  public static AlgorithmDescriptor validate(AlgorithmDescriptor descriptor) {
    Objects.requireNonNull(descriptor, "descriptor");
    validateId(descriptor.id(), "Algorithm");
    validateTypeSignature(descriptor.typeSignature());
    validateContract(descriptor.structureContract(), "Algorithm structure");
    if (descriptor.structureContract().getAnnotation(Structure.class) == null) {
      throw new RegistrationException(
          "Algorithm structure contract must declare @Structure metadata: "
          + descriptor.structureContract().getName());
    }
    validateImplementation(descriptor.implementation(), "Algorithm");
    validateEntryPoint(descriptor);
    validateInitializer(descriptor);
    return descriptor;
  }

  public static void validateUniqueStructureIds(List<StructureDescriptor> descriptors) {
    Objects.requireNonNull(descriptors, "descriptors");
    Set<String> ids = new HashSet<>();
    for (StructureDescriptor descriptor : descriptors) {
      validate(descriptor);
      if (!ids.add(descriptor.id())) {
        throw new RegistrationException("Duplicate Structure id: " + descriptor.id());
      }
    }
  }

  public static void validateUniqueAlgorithmKeys(List<AlgorithmDescriptor> descriptors) {
    Objects.requireNonNull(descriptors, "descriptors");
    Set<AlgorithmKey> keys = new HashSet<>();
    for (AlgorithmDescriptor descriptor : descriptors) {
      validate(descriptor);
      AlgorithmKey key = descriptor.key();
      if (!keys.add(key)) {
        throw new RegistrationException(
            "Duplicate Algorithm registration: structure=" + key.structureContract().getName()
            + ", types=" + key.typeSignature() + ", id=" + key.algorithmId());
      }
    }
  }

  private static void validateEntryPoint(AlgorithmDescriptor descriptor) {
    Method method = descriptor.entryPoint();
    if (method.getAnnotation(AlgorithmEntry.class) == null) {
      throw new RegistrationException(
          "Algorithm entry method is missing @AlgorithmEntry: " + method);
    }
    if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
      throw new RegistrationException(
          "Algorithm entry method must be public and non-static: " + method);
    }
    if (!method.getDeclaringClass().isAssignableFrom(descriptor.implementation())) {
      throw new RegistrationException(
          "Algorithm entry method does not belong to implementation: " + method);
    }
    if (method.isVarArgs() || method.getParameterCount() != 1) {
      throw new RegistrationException("Algorithm entry method must declare exactly one parameter "
                                      + "and it must be a Structure contract: "
          + method);
    }
    Class<?> actualStructure = method.getParameterTypes()[0];
    if (actualStructure.getAnnotation(Structure.class) == null) {
      throw new RegistrationException(
          "Algorithm entry parameter must be a @Structure contract: " + actualStructure.getName());
    }
    if (!actualStructure.isAssignableFrom(descriptor.structureContract())) {
      throw new RegistrationException("Algorithm " + descriptor.id() + " declares structure "
          + descriptor.structureContract().getName() + " but @AlgorithmEntry parameter "
          + actualStructure.getName() + " cannot accept that contract");
    }
  }

  /**
   * Initialization shares the same instance and Structure argument as @AlgorithmEntry.
   * Reject mismatched signatures during discovery instead of failing reflectively at run time.
   */
  private static void validateInitializer(AlgorithmDescriptor descriptor) {
    Method initializer = descriptor.initializer();
    if (initializer == null) return;

    Method entry = descriptor.entryPoint();
    if (!initializer.isAnnotationPresent(AlgorithmInit.class))
      throw new RegistrationException("Algorithm initializer missing @AlgorithmInit: " + initializer);
    if (initializer.equals(entry))
      throw new RegistrationException("@AlgorithmInit and @AlgorithmEntry must mark different methods: "
          + initializer);
    if (!Modifier.isPublic(initializer.getModifiers())
        || Modifier.isStatic(initializer.getModifiers())) {
      throw new RegistrationException("Algorithm initializer must be public and non-static: "
          + initializer);
    }
    if (!initializer.getDeclaringClass().isAssignableFrom(descriptor.implementation())) {
      throw new RegistrationException("Algorithm initializer does not belong to implementation: "
          + initializer);
    }
    if (initializer.getReturnType() != void.class) {
      throw new RegistrationException("Algorithm initializer must return void: " + initializer);
    }
    if (initializer.isVarArgs() || initializer.getParameterCount() != 1
        || !initializer.getGenericParameterTypes()[0]
            .equals(entry.getGenericParameterTypes()[0])) {
      throw new RegistrationException("Algorithm initializer must accept the same Structure "
          + "parameter (including generic type) as @AlgorithmEntry: " + initializer);
    }
  }

  private static void validateId(String id, String component) {
    if (id.isBlank() || !COMPONENT_ID.matcher(id).matches()) {
      throw new RegistrationException(component + " id must be stable kebab-case: '" + id + "'");
    }
  }

  private static void validateTypeSignature(AlgorithmTypeSignature signature) {
    Objects.requireNonNull(signature, "signature");
    for (Class<?> type : signature.types()) {
      if (type.isPrimitive() || type == Void.class || type == void.class) {
        throw new RegistrationException(
            "Algorithm signature types must be non-primitive runtime types: "
                + type.getTypeName());
      }
    }
  }

  private static void validateContract(Class<?> contract, String component) {
    if (!contract.isInterface()) {
      throw new RegistrationException(
          component + " contract must be an interface: " + contract.getName());
    }
  }

  private static void validateImplementation(Class<?> implementation, String component) {
    int modifiers = implementation.getModifiers();
    if (implementation.isInterface() || Modifier.isAbstract(modifiers)) {
      throw new RegistrationException(
          component + " implementation must be concrete: " + implementation.getName());
    }
    if (!Modifier.isPublic(modifiers)) {
      throw new RegistrationException(
          component + " implementation must be public: " + implementation.getName());
    }
    Constructor<?> constructor;
    try {
      constructor = implementation.getDeclaredConstructor();
    } catch (NoSuchMethodException exception) {
      throw new RegistrationException(
          component + " implementation requires a no-arg constructor: " + implementation.getName(),
          exception);
    }
    if (!Modifier.isPublic(constructor.getModifiers())) {
      throw new RegistrationException(component
          + " implementation requires a public no-arg constructor: " + implementation.getName());
    }
  }
}
