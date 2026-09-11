package com.majortom.algorithms.structure.discovery;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
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
        for (Class<?> implementation : scanner.scan(ROOT_PACKAGE, classLoader)) {
            for (Structure annotation : implementation.getAnnotationsByType(Structure.class)) {
                StructureDescriptor descriptor = new StructureDescriptor(
                        annotation.id(), annotation.contract(), implementation);
                descriptors.add(RegistrationValidator.validate(descriptor));
            }
        }
        descriptors.sort(Comparator.comparing(StructureDescriptor::id));
        RegistrationValidator.validateUniqueStructureIds(descriptors);
        return List.copyOf(descriptors);
    }
}
