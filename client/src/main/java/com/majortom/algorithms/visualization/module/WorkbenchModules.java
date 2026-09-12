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
                new WorkbenchModuleDefinition("array", moduleName(registry, "array", "Array"), ArrayController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("linked-list", moduleName(registry, "linked-list", "Linked List"), LinkedListController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("stack", moduleName(registry, "stack", "Stack"), LinearStructureController::stack));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("queue", moduleName(registry, "queue", "Queue"), LinearStructureController::queue));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("tree", moduleName(registry, "tree", "Tree"), TreeController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("graph", moduleName(registry, "graph", "Graph"), GraphController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("string", moduleName(registry, "string", "String"), StringController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("maze", moduleName(registry, "maze", "Maze"), MazeController::new));
        return List.copyOf(modules);
    }

    private static String moduleName(ComponentRegistry registry, String id, String fallbackName) {
        return registry.findStructure(id)
                .map(descriptor -> descriptor.name())
                .orElse(fallbackName);
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
