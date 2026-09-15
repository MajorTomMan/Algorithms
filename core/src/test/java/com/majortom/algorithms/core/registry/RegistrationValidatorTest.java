package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationValidatorTest {
    @Structure(id = "sample", name = "Sample", module = StructureModule.ARRAY, implementation = Component.class)
    interface Contract {}

    @Structure(module = StructureModule.GRAPH)
    interface GraphContract {}

    public static final class Component implements Contract { public Component() {} }
    public static final class AlternateComponent implements Contract { public AlternateComponent() {} }

    public static final class SampleAlgorithm {
        public SampleAlgorithm() {}
        @AlgorithmEntry public void execute(Contract ignored) {}
    }

    public static final class AlternateAlgorithm {
        public AlternateAlgorithm() {}
        @AlgorithmEntry public void execute(Contract ignored) {}
    }

    public static final class GraphAlgorithm {
        public GraphAlgorithm() {}
        @AlgorithmEntry public void execute(GraphContract ignored) {}
    }

    public static final class MultiParameterAlgorithm {
        public MultiParameterAlgorithm() {}
        @AlgorithmEntry public void execute(Contract ignored, int extra) {}
    }

    public static final class MismatchedAlgorithm {
        public MismatchedAlgorithm() {}
        @AlgorithmEntry public void execute(GraphContract ignored) {}
    }

    @Test
    void rejectsDuplicateStructureIds() {
        StructureDescriptor descriptor = new StructureDescriptor(
                "sample", "Sample", StructureModule.ARRAY, Contract.class, Component.class);
        assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validateUniqueStructureIds(List.of(descriptor, descriptor)));
    }

    @Test
    void rejectsPrimitiveAlgorithmValueTypes() {
        AlgorithmDescriptor descriptor = algorithm("sample", int.class, Contract.class, SampleAlgorithm.class);
        assertThrows(RegistrationException.class, () -> RegistrationValidator.validate(descriptor));
    }

    @Test
    void algorithmIdentityIncludesStructureValueTypeAndStableId() {
        AlgorithmDescriptor integer = algorithm("insertion-sort", Integer.class, Contract.class, SampleAlgorithm.class);
        AlgorithmDescriptor string = algorithm("insertion-sort", String.class, Contract.class, SampleAlgorithm.class);
        AlgorithmDescriptor graph = algorithm("insertion-sort", Integer.class, GraphContract.class, GraphAlgorithm.class);

        assertDoesNotThrow(() -> RegistrationValidator.validateUniqueAlgorithmKeys(List.of(integer, string, graph)));

        ComponentRegistry registry = new ComponentRegistry(List.of(), List.of(integer, string, graph));
        assertEquals(3, registry.algorithms().size());
        assertEquals(Integer.class,
                registry.requireAlgorithm(new AlgorithmKey(Contract.class, Integer.class, "insertion-sort")).valueType());
        assertEquals(String.class,
                registry.requireAlgorithm(new AlgorithmKey(Contract.class, String.class, "insertion-sort")).valueType());
        assertEquals(StructureModule.GRAPH,
                registry.requireAlgorithm(new AlgorithmKey(GraphContract.class, Integer.class, "insertion-sort")).module());
    }

    @Test
    void rejectsOnlyExactDuplicateAlgorithmKeys() {
        AlgorithmDescriptor descriptor = algorithm("sample", String.class, Contract.class, SampleAlgorithm.class);
        RegistrationException failure = assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validateUniqueAlgorithmKeys(List.of(descriptor, descriptor)));
        assertTrue(failure.getMessage().contains("structure=" + Contract.class.getName()));
        assertTrue(failure.getMessage().contains("java.lang.String"));
        assertTrue(failure.getMessage().contains("id=sample"));
    }

    @Test
    void descriptorCreatesAndInvokesAlgorithmWithoutBehaviorInterface() {
        AlgorithmDescriptor descriptor = algorithm("sample", Integer.class, Contract.class, AlternateAlgorithm.class);
        assertDoesNotThrow(() -> descriptor.invoke(new Component()));
    }


    @Test
    void rejectsAlgorithmEntryWithMoreThanOneParameter() {
        AlgorithmDescriptor descriptor = algorithm(
                "multi", Integer.class, Contract.class, MultiParameterAlgorithm.class, Contract.class, int.class);
        RegistrationException failure = assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validate(descriptor));
        assertTrue(failure.getMessage().contains("exactly one parameter"));
    }

    @Test
    void rejectsAlgorithmEntryWhoseParameterDoesNotMatchDeclaredStructure() {
        AlgorithmDescriptor descriptor = algorithm(
                "mismatch", Integer.class, Contract.class, MismatchedAlgorithm.class, GraphContract.class);
        RegistrationException failure = assertThrows(RegistrationException.class,
                () -> RegistrationValidator.validate(descriptor));
        assertTrue(failure.getMessage().contains(Contract.class.getName()));
        assertTrue(failure.getMessage().contains(GraphContract.class.getName()));
    }

    @Test
    void structureResolverSupportsExplicitImplementationSelection() {
        StructureDescriptor primary = new StructureDescriptor(
                "primary", "Primary", StructureModule.ARRAY, Contract.class, Component.class);
        StructureDescriptor alternate = new StructureDescriptor(
                "alternate", "Alternate", StructureModule.ARRAY, Contract.class, AlternateComponent.class);
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
            String id, Class<?> valueType, Class<?> structureContract, Class<?> implementation) {
        return algorithm(id, valueType, structureContract, implementation, structureContract);
    }

    private static AlgorithmDescriptor algorithm(
            String id, Class<?> valueType, Class<?> structureContract, Class<?> implementation, Class<?>... entryParameters) {
        try {
            Method entry = implementation.getMethod("execute", entryParameters);
            return new AlgorithmDescriptor(id, "Sample", valueType, structureContract, implementation, entry);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }
}
