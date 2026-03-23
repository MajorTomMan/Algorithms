package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.visualization.BaseController;

import java.util.function.Supplier;

public record AlgorithmModuleDefinition(
        String id,
        String labelKey,
        Supplier<BaseController<?>> controllerFactory) {
}
