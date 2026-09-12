package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.impl.controller.ArrayController;
import com.majortom.algorithms.visualization.impl.controller.GraphController;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureController;
import com.majortom.algorithms.visualization.impl.controller.LinkedListController;
import com.majortom.algorithms.visualization.impl.controller.MazeController;
import com.majortom.algorithms.visualization.impl.controller.StringController;
import com.majortom.algorithms.visualization.impl.controller.TreeController;

import java.util.ArrayList;
import java.util.List;

public final class WorkbenchModules {

    private WorkbenchModules() {
    }

    public static List<WorkbenchModuleDefinition> available(ComponentRegistry registry) {
        List<WorkbenchModuleDefinition> modules = new ArrayList<>();
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("array", "label.structure.array", ArrayController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("linked-list", "module.linked_list", LinkedListController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("stack", "module.stack", LinearStructureController::stack));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("queue", "module.queue", LinearStructureController::queue));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("tree", "module.tree", TreeController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("graph", "module.graph", GraphController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("string", "module.string", StringController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("maze", "module.maze", MazeController::new));
        return List.copyOf(modules);
    }

    private static void addIfAvailable(
            List<WorkbenchModuleDefinition> modules,
            ComponentRegistry registry,
            WorkbenchModuleDefinition definition) {
        String id = definition.id();
        if (registry.hasStructure(id) || registry.hasAlgorithmModule(id)) {
            modules.add(definition);
        }
    }
}
