package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.visualization.BaseController;

import java.util.Objects;
import java.util.function.Supplier;

public record WorkbenchModuleDefinition(
        String id,
        String name,
        FamilyNavigationMetadata navigation,
        Supplier<BaseController<?>> controllerFactory) {

    public WorkbenchModuleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(navigation, "navigation");
        Objects.requireNonNull(controllerFactory, "controllerFactory");
    }

    /**
     * Compatibility constructor for tests/ad-hoc modules; production registrations should provide
     * metadata.
     */
    public WorkbenchModuleDefinition(
            String id, String name, Supplier<BaseController<?>> controllerFactory) {
        this(id, name, new FamilyNavigationMetadata(Integer.MAX_VALUE, "·"), controllerFactory);
    }
}
