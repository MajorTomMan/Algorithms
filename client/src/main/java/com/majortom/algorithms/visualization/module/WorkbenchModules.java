package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.core.registry.ModuleRegistry;
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

    public static List<WorkbenchModuleDefinition> available(ModuleRegistry registry) {
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
                new WorkbenchModuleDefinition("maze", "module.maze", MazeController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("tree", "module.tree", TreeController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("graph", "module.graph", GraphController::new));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("string", "module.string", StringController::new));
        return List.copyOf(modules);
    }

    private static void addIfAvailable(
            List<WorkbenchModuleDefinition> modules,
            ModuleRegistry registry,
            WorkbenchModuleDefinition definition) {
        String id = definition.id();
        if (registry.hasStructureFamily(id) || registry.hasAlgorithmFamily(id) || isMazeAvailable(registry, id)) {
            modules.add(definition);
        }
    }

    private static boolean isMazeAvailable(ModuleRegistry registry, String id) {
        if (!"maze".equals(id)) {
            return false;
        }
        if (registry.hasAlgorithmFamily("maze")) {
            return true;
        }
        for (String valueType : registry.algorithmValueTypes("graph")) {
            for (String algorithmId : registry.algorithmIds("graph", valueType)) {
                if (algorithmId.startsWith("graph-generator-")) {
                    return true;
                }
            }
        }
        return false;
    }
}
