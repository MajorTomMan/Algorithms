package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.visualization.impl.controller.GraphController;
import com.majortom.algorithms.visualization.impl.controller.MazeController;
import com.majortom.algorithms.visualization.impl.controller.SortController;
import com.majortom.algorithms.visualization.impl.controller.TreeController;

import java.util.List;

public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static List<AlgorithmModuleDefinition> defaults() {
        return List.of(
                new AlgorithmModuleDefinition("sort", "module.sort", SortController::new),
                new AlgorithmModuleDefinition("maze", "module.maze", MazeController::new),
                new AlgorithmModuleDefinition("tree", "module.tree", TreeController::new),
                new AlgorithmModuleDefinition("graph", "module.graph", GraphController::new));
    }
}
