package com.majortom.algorithms.core.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationValidatorTest {
    interface Contract {}
    public static final class Component implements Contract { public Component() {} }
    public static final class AlternateComponent implements Contract { public AlternateComponent() {} }

    @Test
    void rejectsDuplicateStructureIds() {
        StructureDescriptor descriptor = new StructureDescriptor("sample", "Sample", Contract.class, Component.class);
        assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validateUniqueStructureIds(List.of(descriptor, descriptor)));
    }

    @Test
    void rejectsPrimitiveAlgorithmValueTypes() {
        AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
                "sample", "Sample", "array", int.class, Contract.class, Component.class);
        assertThrows(RegistrationException.class, () -> RegistrationValidator.validate(descriptor));
    }

    @Test
    void algorithmIdentityIncludesModuleValueTypeAndStableId() {
        AlgorithmDescriptor integer = algorithm("insertion-sort", "array", Integer.class, Component.class);
        AlgorithmDescriptor string = algorithm("insertion-sort", "array", String.class, Component.class);
        AlgorithmDescriptor graph = algorithm("insertion-sort", "graph", Integer.class, Component.class);

        assertDoesNotThrow(() -> RegistrationValidator.validateUniqueAlgorithmKeys(
                List.of(integer, string, graph)));

        ComponentRegistry registry = new ComponentRegistry(List.of(), List.of(integer, string, graph));
        assertEquals(3, registry.algorithms().size());
        assertEquals(Integer.class,
                registry.requireAlgorithm("array", Integer.class, "insertion-sort").valueType());
        assertEquals(String.class,
                registry.requireAlgorithm("array", String.class, "insertion-sort").valueType());
        assertEquals("graph",
                registry.requireAlgorithm("graph", Integer.class, "insertion-sort").moduleId());
    }

    @Test
    void rejectsOnlyExactDuplicateAlgorithmKeys() {
        AlgorithmDescriptor descriptor = algorithm("sample", "array", String.class, Component.class);
        RegistrationException failure = assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validateUniqueAlgorithmKeys(List.of(descriptor, descriptor)));
        assertTrue(failure.getMessage().contains("module=array"));
        assertTrue(failure.getMessage().contains("java.lang.String"));
        assertTrue(failure.getMessage().contains("id=sample"));
    }

    @Test
    void registryCreatesAlgorithmUsingCompositeIdentity() {
        AlgorithmDescriptor integer = algorithm("sample", "array", Integer.class, Component.class);
        AlgorithmDescriptor string = algorithm("sample", "array", String.class, AlternateComponent.class);
        ComponentRegistry registry = new ComponentRegistry(List.of(), List.of(integer, string));

        assertInstanceOf(Component.class,
                registry.createAlgorithm("array", Integer.class, "sample", Contract.class));
        assertInstanceOf(AlternateComponent.class,
                registry.createAlgorithm("array", String.class, "sample", Contract.class));
    }

    @Test
    void structureResolverSupportsExplicitImplementationSelection() {
        StructureDescriptor primary = new StructureDescriptor("primary", "Primary", Contract.class, Component.class);
        StructureDescriptor alternate = new StructureDescriptor("alternate", "Alternate", Contract.class, AlternateComponent.class);
        StructureResolver resolver = new StructureResolver(
                new ComponentRegistry(List.of(primary, alternate), List.of()));

        assertThrows(RegistrationException.class, () -> resolver.resolve(Contract.class));
        assertEquals(AlternateComponent.class,
                resolver.resolve(Contract.class, "alternate").implementation());
        assertInstanceOf(Component.class, resolver.create(Contract.class, "primary"));

        RegistrationException failure = assertThrows(RegistrationException.class,
                () -> resolver.resolve(Contract.class, "missing"));
        assertTrue(failure.getMessage().contains("missing"));
        assertTrue(failure.getMessage().contains("alternate"));
        assertTrue(failure.getMessage().contains("primary"));
    }

    private static AlgorithmDescriptor algorithm(
            String id, String moduleId, Class<?> valueType, Class<?> implementation) {
        return new AlgorithmDescriptor(id, "Sample", moduleId, valueType, Void.class, implementation);
    }
}
