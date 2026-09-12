package com.majortom.algorithms.visualization.structure;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.registry.ComponentRegistry;

/** Component metadata access for Structure display names. */
public final class StructureCatalog {

    private static final ComponentRegistry REGISTRY = ComponentDiscovery.discover();

    private StructureCatalog() {
    }

    public static String name(String structureId) {
        return REGISTRY.findStructure(structureId)
                .orElseThrow(() -> new IllegalArgumentException("No Structure registered for id: " + structureId))
                .name();
    }
}
