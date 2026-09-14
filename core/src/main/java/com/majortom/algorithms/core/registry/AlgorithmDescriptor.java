package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public record AlgorithmDescriptor(
        String id,
        String name,
        Class<?> valueType,
        Class<?> structureContract,
        Class<?> implementation,
        Method entryPoint) {

    public AlgorithmDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(structureContract, "structureContract");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(entryPoint, "entryPoint");
    }

    public AlgorithmKey key() {
        return AlgorithmKey.of(this);
    }

    public StructureModule module() {
        Structure metadata = structureContract.getAnnotation(Structure.class);
        if (metadata == null) {
            throw new RegistrationException("Algorithm structure contract is missing @Structure metadata: "
                    + structureContract.getName());
        }
        return metadata.module();
    }

    public Object newInstance() {
        try {
            return implementation.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new RegistrationException("Unable to instantiate Algorithm " + id + " using "
                    + implementation.getName(), exception);
        }
    }

    public Object invoke(Object... arguments) {
        Object instance = newInstance();
        try {
            return entryPoint.invoke(instance, arguments);
        } catch (IllegalAccessException exception) {
            throw new RegistrationException("Unable to access Algorithm entry " + implementation.getName()
                    + "#" + entryPoint.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RegistrationException("Algorithm entry failed: " + implementation.getName()
                    + "#" + entryPoint.getName(), cause);
        }
    }
}
