package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.structure.discovery.StructureDiscovery;

import java.util.Objects;

public final class ComponentDiscovery {

    private ComponentDiscovery() {
    }

    public static ComponentRegistry discover() {
        return Holder.DEFAULT;
    }

    public static ComponentRegistry discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        return new ComponentRegistry(
                new StructureDiscovery().discover(classLoader),
                new AlgorithmDiscovery().discover(classLoader));
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ComponentDiscovery.class.getClassLoader();
        }
        return classLoader;
    }

    private static final class Holder {
        private static final ComponentRegistry DEFAULT = discover(defaultClassLoader());
    }
}
