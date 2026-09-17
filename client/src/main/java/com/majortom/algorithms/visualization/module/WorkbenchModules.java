package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.impl.controller.ArrayController;
import com.majortom.algorithms.visualization.impl.controller.GraphController;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureController;
import com.majortom.algorithms.visualization.impl.controller.LinkedListController;
import com.majortom.algorithms.visualization.impl.controller.MazeController;
import com.majortom.algorithms.visualization.impl.controller.StringController;
import com.majortom.algorithms.visualization.impl.controller.TreeController;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;

import java.util.ArrayList;
import java.util.List;

public final class WorkbenchModules {

    private WorkbenchModules() {
    }

    public static List<WorkbenchModuleDefinition> available(ComponentRegistry registry, RenderContext renderContext) {
        List<WorkbenchModuleDefinition> modules = new ArrayList<>();
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("array", moduleName(registry, "array", "Array"), new FamilyNavigationMetadata(10, "▦"), () -> new ArrayController(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("linked-list", moduleName(registry, "linked-list", "Linked List"), new FamilyNavigationMetadata(20, "⌁"), () -> new LinkedListController(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("stack", moduleName(registry, "stack", "Stack"), new FamilyNavigationMetadata(30, "▤"), () -> LinearStructureController.stack(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("queue", moduleName(registry, "queue", "Queue"), new FamilyNavigationMetadata(40, "▥"), () -> LinearStructureController.queue(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("tree", moduleName(registry, "tree", "Tree"), new FamilyNavigationMetadata(50, "⌘"), () -> new TreeController(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("graph", moduleName(registry, "graph", "Graph"), new FamilyNavigationMetadata(60, "◇"), () -> new GraphController(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("string", moduleName(registry, "string", "String"), new FamilyNavigationMetadata(70, "Aa"), () -> new StringController(renderContext)));
        addIfAvailable(modules, registry,
                new WorkbenchModuleDefinition("maze", moduleName(registry, "maze", "Maze"), new FamilyNavigationMetadata(80, "▧"), () -> new MazeController(renderContext)));
        modules.sort(java.util.Comparator.comparingInt(module -> module.navigation().order()));
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
        if (registry.hasStructure(id) || registry.hasAlgorithmModule(StructureModule.fromId(id))) {
            modules.add(definition);
        }
    }
}
