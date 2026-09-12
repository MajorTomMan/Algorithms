package com.majortom.algorithms.core.registry;

import java.util.List;
import java.util.Objects;

public final class StructureResolver {
    private final ComponentRegistry registry;

    public StructureResolver(ComponentRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public StructureDescriptor resolve(AlgorithmDescriptor algorithm) {
        Objects.requireNonNull(algorithm, "algorithm");
        return resolve(requireStructureContract(algorithm));
    }

    public StructureDescriptor resolve(AlgorithmDescriptor algorithm, String structureId) {
        Objects.requireNonNull(algorithm, "algorithm");
        return resolve(requireStructureContract(algorithm), structureId);
    }

    public StructureDescriptor resolve(Class<?> requiredContract) {
        validateContract(requiredContract);
        List<StructureDescriptor> exact = exact(requiredContract);
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (exact.size() > 1) {
            throw ambiguous(requiredContract, exact);
        }
        List<StructureDescriptor> compatible = compatible(requiredContract);
        if (compatible.size() == 1) {
            return compatible.getFirst();
        }
        if (compatible.isEmpty()) {
            throw new RegistrationException("No Structure registered for contract: " + requiredContract.getName());
        }
        throw ambiguous(requiredContract, compatible);
    }

    public StructureDescriptor resolve(Class<?> requiredContract, String structureId) {
        validateContract(requiredContract);
        String requestedId = requireId(structureId);
        List<StructureDescriptor> candidates = allMatches(requiredContract);
        return candidates.stream()
                .filter(descriptor -> descriptor.id().equals(requestedId))
                .findFirst()
                .orElseThrow(() -> new RegistrationException("No Structure registered for contract "
                        + requiredContract.getName() + " with id '" + requestedId + "'; available ids: "
                        + ids(candidates)));
    }

    public <S> S create(Class<S> requiredContract) {
        StructureDescriptor descriptor = resolve(requiredContract);
        return registry.createStructure(descriptor.id(), requiredContract);
    }

    public <S> S create(Class<S> requiredContract, String structureId) {
        StructureDescriptor descriptor = resolve(requiredContract, structureId);
        return registry.createStructure(descriptor.id(), requiredContract);
    }

    private Class<?> requireStructureContract(AlgorithmDescriptor algorithm) {
        if (!algorithm.hasStructureContract()) {
            throw new RegistrationException("Algorithm " + algorithm.key()
                    + " does not declare a Structure contract");
        }
        return algorithm.structureContract();
    }

    private void validateContract(Class<?> requiredContract) {
        Objects.requireNonNull(requiredContract, "requiredContract");
        if (!requiredContract.isInterface()) {
            throw new IllegalArgumentException("requiredContract must be an interface: " + requiredContract.getName());
        }
    }

    private List<StructureDescriptor> exact(Class<?> requiredContract) {
        return registry.structures().stream()
                .filter(descriptor -> descriptor.contract().equals(requiredContract))
                .toList();
    }

    private List<StructureDescriptor> compatible(Class<?> requiredContract) {
        return registry.structures().stream()
                .filter(descriptor -> requiredContract.isAssignableFrom(descriptor.contract()))
                .filter(descriptor -> requiredContract.isAssignableFrom(descriptor.implementation()))
                .toList();
    }

    private List<StructureDescriptor> allMatches(Class<?> requiredContract) {
        return registry.structures().stream()
                .filter(descriptor -> descriptor.contract().equals(requiredContract)
                        || (requiredContract.isAssignableFrom(descriptor.contract())
                        && requiredContract.isAssignableFrom(descriptor.implementation())))
                .toList();
    }

    private RegistrationException ambiguous(Class<?> contract, List<StructureDescriptor> candidates) {
        return new RegistrationException("Multiple Structures match contract " + contract.getName() + ": "
                + ids(candidates) + "; select one by structure id");
    }

    private List<String> ids(List<StructureDescriptor> descriptors) {
        return descriptors.stream().map(StructureDescriptor::id).sorted().toList();
    }

    private String requireId(String id) {
        Objects.requireNonNull(id, "structureId");
        if (id.isBlank()) {
            throw new IllegalArgumentException("structureId must not be blank");
        }
        return id;
    }
}
