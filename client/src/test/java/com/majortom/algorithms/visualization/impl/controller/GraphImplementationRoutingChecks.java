package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.module.WorkbenchModules;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Headless regression: both graph implementations share a single family,
 * but the algorithm menus contain only algorithms compatible with that implementation.
 */
public final class GraphImplementationRoutingChecks {
    public static void main(String[] args) throws Exception {
        var registry = ComponentDiscovery.discover();
        var modules = WorkbenchModules.available(registry, null);
        long graphCount = modules.stream()
                .filter(module -> StructureIds.GRAPH.equals(module.id())).count();
        if (graphCount != 1 || modules.stream()
                .anyMatch(module -> StructureIds.MULTI_GRAPH.equals(module.id())))
            throw new AssertionError("Graph and MultiGraph must share one navigation family");

        var signature = AlgorithmTypeSignature.of(Integer.class);
        var simple = new WorkbenchModuleNavigation(
                modules, null, unused -> {}, unused -> signature,
                () -> GraphImplementation.SIMPLE);
        var multi = new WorkbenchModuleNavigation(
                modules, null, unused -> {}, unused -> signature,
                () -> GraphImplementation.MULTI);

        List<String> simpleIds = simple.algorithmNavigationItems(StructureIds.GRAPH)
                .stream().map(AlgorithmNavigationItem::id).toList();
        List<String> multiIds = multi.algorithmNavigationItems(StructureIds.GRAPH)
                .stream().map(AlgorithmNavigationItem::id).toList();

        if (simpleIds.isEmpty() || multiIds.isEmpty())
            throw new AssertionError("Both graph modes need discoverable algorithms");
        if (!multiIds.contains("multi-graph-bfs") || !multiIds.contains("multi-graph-dfs"))
            throw new AssertionError("MultiGraph BFS/DFS missing");
        if (simpleIds.contains("multi-graph-bfs") || simpleIds.contains("multi-graph-dfs"))
            throw new AssertionError("MultiGraph algorithms leaked into simple Graph");
        for (String id : multiIds) {
            if (simpleIds.contains(id))
                throw new AssertionError("Algorithm leaked across graph contracts: " + id);
        }
        for (String id : AlgorithmCatalog.forWorkbenchModule(StructureIds.GRAPH, signature))
            if (id.startsWith("multi-graph-"))
                throw new AssertionError("Graph catalog accidentally includes MultiGraph algorithms");

        checkSelector("fxml/GraphControls.fxml");
        checkSelector("fxml/MultiGraphControls.fxml");
        System.out.println("GraphImplementationRoutingChecks passed");
    }

    private static void checkSelector(String resource) throws Exception {
        try (InputStream input = GraphImplementationRoutingChecks.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (input == null || !new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .contains("fx:id=\"implementationSelector\""))
                throw new AssertionError("Graph implementation selector missing: " + resource);
        }
    }
}
