package com.majortom.algorithms.core.registry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class RegistrationValidator {

    private static final Pattern COMPONENT_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private RegistrationValidator() {
    }

    public static StructureDescriptor validate(StructureDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        validateId(descriptor.id(), "Structure");
        validateContract(descriptor.contract(), "Structure");
        validateImplementation(descriptor.implementation(), "Structure");
        if (!descriptor.contract().isAssignableFrom(descriptor.implementation())) {
            throw new RegistrationException("Structure implementation " + descriptor.implementation().getName()
                    + " does not implement declared contract " + descriptor.contract().getName());
        }
        return descriptor;
    }

    public static AlgorithmDescriptor validate(AlgorithmDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        validateId(descriptor.id(), "Algorithm");
        validateId(descriptor.moduleId(), "Algorithm module");
        validateValueType(descriptor.valueType());
        if (descriptor.hasStructureContract()) {
            validateContract(descriptor.structureContract(), "Algorithm structure");
        }
        validateImplementation(descriptor.implementation(), "Algorithm");
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
                throw new RegistrationException("Duplicate Algorithm registration: module=" + key.moduleId()
                        + ", type=" + key.valueType().getName() + ", id=" + key.algorithmId());
            }
        }
    }

    private static void validateId(String id, String component) {
        if (id.isBlank() || !COMPONENT_ID.matcher(id).matches()) {
            throw new RegistrationException(component + " id must be stable kebab-case: '" + id + "'");
        }
    }

    private static void validateValueType(Class<?> valueType) {
        if (valueType.isPrimitive() || valueType == Void.class || valueType == void.class) {
            throw new RegistrationException("Algorithm value type must be a non-primitive runtime type: "
                    + valueType.getTypeName());
        }
    }

    private static void validateContract(Class<?> contract, String component) {
        if (!contract.isInterface()) {
            throw new RegistrationException(component + " contract must be an interface: " + contract.getName());
        }
    }

    private static void validateImplementation(Class<?> implementation, String component) {
        int modifiers = implementation.getModifiers();
        if (implementation.isInterface() || Modifier.isAbstract(modifiers)) {
            throw new RegistrationException(component + " implementation must be concrete: "
                    + implementation.getName());
        }
        if (!Modifier.isPublic(modifiers)) {
            throw new RegistrationException(component + " implementation must be public: "
                    + implementation.getName());
        }
        Constructor<?> constructor;
        try {
            constructor = implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException exception) {
            throw new RegistrationException(component + " implementation requires a no-arg constructor: "
                    + implementation.getName(), exception);
        }
        if (!Modifier.isPublic(constructor.getModifiers())) {
            throw new RegistrationException(component + " implementation requires a public no-arg constructor: "
                    + implementation.getName());
        }
    }
}
