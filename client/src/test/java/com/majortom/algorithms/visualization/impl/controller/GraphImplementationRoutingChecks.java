package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.module.WorkbenchModules;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** A single graph family, multigraph topology and no implementation switch. */
public final class GraphImplementationRoutingChecks {
    public static void main(String[] args) throws Exception {
        var registry = ComponentDiscovery.discover();
        var modules = WorkbenchModules.available(registry, null);
        if (modules.stream().filter(m -> StructureIds.GRAPH.equals(m.id())).count() != 1)
            throw new AssertionError("Exactly one Graph family is required");
        var navigation = new WorkbenchModuleNavigation(modules, null,
                unused -> {}, unused -> AlgorithmTypeSignature.of(Integer.class));
        List<String> ids = navigation.algorithmNavigationItems(StructureIds.GRAPH)
                .stream().map(AlgorithmNavigationItem::id).toList();
        if (!ids.contains("graph-bfs") || !ids.contains("graph-dfs")
                || !ids.contains("kruskal-minimum-spanning"))
            throw new AssertionError("Unified Graph algorithms not discovered: " + ids);
        if (!AlgorithmCatalog.graphAlgorithms(Object.class).contains("graph-bfs"))
            throw new AssertionError("Generic Graph traversal missing");
        try (InputStream input = GraphImplementationRoutingChecks.class.getClassLoader()
                .getResourceAsStream("fxml/GraphControls.fxml")) {
            if (input == null) throw new AssertionError("Graph controls missing");
            String fxml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            if (fxml.contains("implementationSelector")
                    || !fxml.contains("structureSelector"))
                throw new AssertionError("Only direction selector should remain");
        }
        System.out.println("GraphImplementationRoutingChecks passed");
    }
}
