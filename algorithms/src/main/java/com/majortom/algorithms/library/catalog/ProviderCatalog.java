package com.majortom.algorithms.library.catalog;

import com.majortom.algorithms.core.api.AlgorithmInvoker;
import com.majortom.algorithms.core.api.AlgorithmMetadata;
import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.library.graph.GraphBfs;
import com.majortom.algorithms.library.graph.GraphBfsInput;
import com.majortom.algorithms.library.graph.GraphBfsOutput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationOutput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathInput;
import com.majortom.algorithms.library.maze.ArrayMazePathOutput;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeBfsGenerator;
import com.majortom.algorithms.library.maze.GraphMazeGenerationInput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationOutput;
import com.majortom.algorithms.library.sort.IntegerHeapSort;
import com.majortom.algorithms.library.sort.IntegerQuickSort;
import com.majortom.algorithms.library.sort.IntegerSelectionSort;
import com.majortom.algorithms.library.sort.insertion.IntegerInsertionSort;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.library.tree.AvlTreeOutput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** The single explicit registration point for production typed algorithms. */
public final class ProviderCatalog {

    private static final String VERSION = "2.0";
    private static final ProviderCatalog PRODUCTION = new ProviderCatalog(productionProviders());

    private final List<AlgorithmProvider<?, ?>> providers;
    private final Map<String, AlgorithmProvider<?, ?>> providersById;

    public ProviderCatalog(List<? extends AlgorithmProvider<?, ?>> providers) {
        Objects.requireNonNull(providers, "providers");
        this.providers = List.copyOf(providers);
        Map<String, AlgorithmProvider<?, ?>> indexed = new LinkedHashMap<>();
        for (AlgorithmProvider<?, ?> provider : this.providers) {
            Objects.requireNonNull(provider, "provider");
            String id = provider.metadata().id();
            if (indexed.putIfAbsent(id, provider) != null) {
                throw new IllegalArgumentException("Duplicate algorithm provider ID: " + id);
            }
        }
        providersById = Map.copyOf(indexed);
    }

    public static ProviderCatalog production() {
        return PRODUCTION;
    }

    public List<AlgorithmProvider<?, ?>> providers() {
        return providers;
    }

    public List<AlgorithmInvoker> invokers() {
        return providers.stream().map(AlgorithmProvider::invoker).toList();
    }

    public Optional<AlgorithmProvider<?, ?>> find(String id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(providersById.get(id));
    }

    public AlgorithmProvider<?, ?> require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown algorithm provider ID: " + id));
    }

    private static List<AlgorithmProvider<?, ?>> productionProviders() {
        return List.of(
                sort("insertion-sort", IntegerInsertionSort::new),
                sort("selection-sort", IntegerSelectionSort::new),
                sort("quick-sort", IntegerQuickSort::new),
                sort("heap-sort", IntegerHeapSort::new),
                mazeGenerator("maze-generator-bfs", ArrayMazeGenerator.Strategy.BFS),
                mazeGenerator("maze-generator-dfs", ArrayMazeGenerator.Strategy.DFS),
                mazeGenerator("maze-generator-union-find", ArrayMazeGenerator.Strategy.UNION_FIND),
                provider(
                        "graph-generator-bfs", "maze",
                        GraphMazeGenerationInput.class, GraphMazeGenerationOutput.class,
                        GraphMazeBfsGenerator::new),
                mazePathfinder("maze-pathfinder-astar", ArrayMazePathfinder.Strategy.ASTAR),
                mazePathfinder("maze-pathfinder-dfs", ArrayMazePathfinder.Strategy.DFS),
                provider("tree-avl", "tree", AvlTreeInput.class, AvlTreeOutput.class, AvlTreeCommands::new),
                provider("graph-bfs", "graph", GraphBfsInput.class, GraphBfsOutput.class, GraphBfs::new));
    }

    private static AlgorithmProvider<IntegerSortInput, IntegerSortOutput> sort(
            String id,
            java.util.function.Supplier<? extends com.majortom.algorithms.core.api.Algorithm<
                    IntegerSortInput, IntegerSortOutput>> factory) {
        return provider(id, "sort", IntegerSortInput.class, IntegerSortOutput.class, factory);
    }

    private static AlgorithmProvider<ArrayMazeGenerationInput, ArrayMazeGenerationOutput> mazeGenerator(
            String id,
            ArrayMazeGenerator.Strategy strategy) {
        return provider(
                id, "maze", ArrayMazeGenerationInput.class, ArrayMazeGenerationOutput.class,
                () -> new ArrayMazeGenerator(strategy));
    }

    private static AlgorithmProvider<ArrayMazePathInput, ArrayMazePathOutput> mazePathfinder(
            String id,
            ArrayMazePathfinder.Strategy strategy) {
        return provider(
                id, "maze", ArrayMazePathInput.class, ArrayMazePathOutput.class,
                () -> new ArrayMazePathfinder(strategy));
    }

    private static <I extends com.majortom.algorithms.core.api.AlgorithmInput,
            O extends com.majortom.algorithms.core.api.AlgorithmOutput>
            AlgorithmProvider<I, O> provider(
                    String id,
                    String moduleId,
                    Class<I> inputType,
                    Class<O> outputType,
                    java.util.function.Supplier<? extends com.majortom.algorithms.core.api.Algorithm<I, O>> factory) {
        return new StandardAlgorithmProvider<>(
                new AlgorithmMetadata(id, moduleId, VERSION), inputType, outputType, factory);
    }
}
