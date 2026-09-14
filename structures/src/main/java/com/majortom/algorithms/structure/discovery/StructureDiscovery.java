package com.majortom.algorithms.structure.discovery;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.ComponentNames;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;
import com.majortom.algorithms.core.registry.StructureDescriptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class StructureDiscovery {

    public static final String ROOT_PACKAGE = "com.majortom.algorithms.structure";

    private final FrameworkClassScanner scanner;

    public StructureDiscovery() {
        this(new FrameworkClassScanner());
    }

    StructureDiscovery(FrameworkClassScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public List<StructureDescriptor> discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        List<StructureDescriptor> descriptors = new ArrayList<>();
        for (Class<?> contract : scanner.scan(ROOT_PACKAGE, classLoader)) {
            Structure annotation = contract.getAnnotation(Structure.class);
            if (annotation == null) {
                continue;
            }
            if (!contract.isInterface()) {
                throw new RegistrationException("@Structure must be declared on a capability interface: "
                        + contract.getName());
            }
            boolean hasId = !annotation.id().isBlank();
            boolean hasImplementation = annotation.implementation() != Void.class;
            if (!hasId && !hasImplementation) {
                continue;
            }
            if (hasId != hasImplementation) {
                throw new RegistrationException("Concrete @Structure registration requires both id and implementation: "
                        + contract.getName());
            }
            Class<?> implementation = annotation.implementation();
            StructureDescriptor descriptor = new StructureDescriptor(
                    annotation.id(), ComponentNames.resolve(annotation.name(), implementation),
                    annotation.module(), contract, implementation);
            descriptors.add(RegistrationValidator.validate(descriptor));
        }
        descriptors.sort(Comparator.comparing(StructureDescriptor::id));
        RegistrationValidator.validateUniqueStructureIds(descriptors);
        return List.copyOf(descriptors);
    }
}
