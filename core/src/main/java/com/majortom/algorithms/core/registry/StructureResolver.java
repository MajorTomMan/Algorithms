package com.majortom.algorithms.core.registry;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public final class StructureResolver {
    private final ComponentRegistry registry;

    public StructureResolver(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public StructureDescriptor resolve(AlgorithmDescriptor algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        return resolve(algorithm.structureContract());
    }

    public StructureDescriptor resolve(Class<?> requiredContract) {
        Objects.requireNonNull(requiredContract, "requiredContract");
        if (!requiredContract.isInterface()) {
            throw new IllegalArgumentException("requiredContract must be an interface: " + requiredContract.getName());
        }
        List<StructureDescriptor> exact = registry.structures().stream()
                .filter(descriptor -> descriptor.contract().equals(requiredContract))
                .toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (exact.size() > 1) {
            throw ambiguous(requiredContract, exact);
        }
        List<StructureDescriptor> compatible = registry.structures().stream()
                .filter(descriptor -> requiredContract.isAssignableFrom(descriptor.contract()))
                .filter(descriptor -> requiredContract.isAssignableFrom(descriptor.implementation()))
                .toList();
        if (compatible.size() == 1) {
            return compatible.getFirst();
        }
        if (compatible.isEmpty()) {
            throw new RegistrationException("No Structure registered for contract: " + requiredContract.getName());
        }
        throw ambiguous(requiredContract, compatible);
    }

    public Object create(AlgorithmDescriptor algorithm) {
        return instantiate(resolve(algorithm));
    }

    public <S> S create(Class<S> requiredContract) {
        StructureDescriptor descriptor = resolve(requiredContract);
        return requiredContract.cast(instantiate(descriptor));
    }

    private Object instantiate(StructureDescriptor descriptor) {
        try {
            return descriptor.implementation().getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new RegistrationException("Unable to instantiate Structure " + descriptor.id()
                    + " using " + descriptor.implementation().getName(), exception);
        }
    }

    private RegistrationException ambiguous(Class<?> contract, List<StructureDescriptor> candidates) {
        return new RegistrationException("Multiple Structures match contract " + contract.getName() + ": "
                + candidates.stream().map(StructureDescriptor::id).sorted().toList());
    }
}
