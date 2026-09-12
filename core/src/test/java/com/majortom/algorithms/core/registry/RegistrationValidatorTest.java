package com.majortom.algorithms.core.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationValidatorTest {
    interface Contract {}
    public static final class Component implements Contract { public Component() {} }

    @Test
    void rejectsDuplicateStableIds() {
        StructureDescriptor descriptor = new StructureDescriptor("sample", Contract.class, Component.class);
        assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validateUniqueStructureIds(List.of(descriptor, descriptor)));
    }

    @Test
    void rejectsPrimitiveAlgorithmValueTypes() {
        AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
                "sample", "array", int.class, Contract.class, Component.class);
        assertThrows(RegistrationException.class, () -> RegistrationValidator.validate(descriptor));
    }
}
