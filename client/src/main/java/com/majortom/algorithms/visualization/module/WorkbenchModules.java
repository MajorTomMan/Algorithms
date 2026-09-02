package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.visualization.impl.controller.GraphController;
import com.majortom.algorithms.visualization.impl.controller.HashTableController;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureController;
import com.majortom.algorithms.visualization.impl.controller.MazeController;
import com.majortom.algorithms.visualization.impl.controller.SortController;
import com.majortom.algorithms.visualization.impl.controller.StringController;
import com.majortom.algorithms.visualization.impl.controller.TreeController;

import java.util.List;

public final class WorkbenchModules {
    private WorkbenchModules() {}

    public static List<WorkbenchModuleDefinition> defaults() {
        return List.of(
                new WorkbenchModuleDefinition("sort", "module.sort", SortController::new),
                new WorkbenchModuleDefinition("linked-list", "module.linked_list", LinearStructureController::linkedList),
                new WorkbenchModuleDefinition("stack", "module.stack", LinearStructureController::stack),
                new WorkbenchModuleDefinition("queue", "module.queue", LinearStructureController::queue),
                new WorkbenchModuleDefinition("maze", "module.maze", MazeController::new),
                new WorkbenchModuleDefinition("tree", "module.tree", TreeController::new),
                new WorkbenchModuleDefinition("graph", "module.graph", GraphController::new),
                new WorkbenchModuleDefinition("hash-table", "module.hash_table", HashTableController::new),
                new WorkbenchModuleDefinition("string", "module.string", StringController::new));
    }
}
