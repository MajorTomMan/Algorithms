package com.majortom.algorithms.library;

import com.majortom.algorithms.core.api.AlgorithmProvider;
import com.majortom.algorithms.core.runtime.DefaultAlgorithmRunner;
import com.majortom.algorithms.core.runtime.ExecutionEvent;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatus;
import com.majortom.algorithms.core.runtime.InMemoryEventSink;
import com.majortom.algorithms.library.catalog.ProviderCatalog;
import com.majortom.algorithms.library.graph.GraphBfsEvent;
import com.majortom.algorithms.library.graph.GraphBfsInput;
import com.majortom.algorithms.library.graph.GraphBfsOutput;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.graph.IntGraph;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationEvent;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationOutput;
import com.majortom.algorithms.library.maze.ArrayMazePathInput;
import com.majortom.algorithms.library.maze.ArrayMazePathOutput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeGenerationEvent;
import com.majortom.algorithms.library.maze.GraphMazeGenerationInput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationOutput;
import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.library.maze.GridPoint;
import com.majortom.algorithms.library.sort.event.SortCompletedEvent;
import com.majortom.algorithms.library.sort.event.SortInitializedEvent;
import com.majortom.algorithms.library.sort.event.SortSwappedEvent;
import com.majortom.algorithms.library.sort.event.SortWrittenEvent;
import com.majortom.algorithms.library.sort.model.IntegerSortInput;
import com.majortom.algorithms.library.sort.model.IntegerSortOutput;
import com.majortom.algorithms.library.tree.AvlCommand;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeEvent;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.library.tree.AvlTreeOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionAlgorithmsTest {

    private static final Set<String> PRODUCTION_IDS = Set.of(
            "insertion-sort", "selection-sort", "quick-sort", "heap-sort",
            "maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find",
            "graph-generator-bfs", "maze-pathfinder-astar", "maze-pathfinder-dfs",
            "tree-avl", "graph-bfs");

    @Test
    void productionCatalogHasExactlyTwelveUniqueTypedProviders() {
        ProviderCatalog catalog = ProviderCatalog.production();
        assertEquals(12, catalog.providers().size());
        assertEquals(PRODUCTION_IDS, catalog.providers().stream()
                .map(provider -> provider.metadata().id()).collect(java.util.stream.Collectors.toSet()));
        for (AlgorithmProvider<?, ?> provider : catalog.providers()) {
            assertNotNull(provider.inputType());
            assertNotNull(provider.outputType());
            assertNotNull(provider.createAlgorithm());
        }
        assertFalse(catalog.find("maze-pathfinder-bfs").isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> new ProviderCatalog(List.of(catalog.providers().getFirst(), catalog.providers().getFirst())));
    }

    @Test
    void allFourSortsAreCorrectAndReplayable() {
        for (String id : List.of("insertion-sort", "selection-sort", "quick-sort", "heap-sort")) {
            InMemoryEventSink sink = new InMemoryEventSink();
            IntegerSortOutput output = (IntegerSortOutput) run(id,
                    new IntegerSortInput(List.of(9, -2, 4, 4, 0, 1)), sink);
            assertEquals(List.of(-2, 0, 1, 4, 4, 9), output.values(), id);
            assertEquals(output.values(), replaySort(sink.events()), id);
        }
        List<Integer> monotonic = java.util.stream.IntStream.range(0, 20_000).boxed().toList();
        IntegerSortOutput quick = (IntegerSortOutput) run(
                "quick-sort", new IntegerSortInput(monotonic), new InMemoryEventSink());
        assertEquals(monotonic, quick.values());
        List<Integer> duplicates = java.util.Collections.nCopies(20_000, 7);
        IntegerSortOutput duplicateQuick = (IntegerSortOutput) run(
                "quick-sort", new IntegerSortInput(duplicates), new InMemoryEventSink());
        assertEquals(duplicates, duplicateQuick.values());
    }

    @Test
    void mazeDimensionsRejectOverflowAndExcessiveAllocation() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArrayMazeGenerationInput(Integer.MAX_VALUE, Integer.MAX_VALUE, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new GraphMazeGenerationInput(Integer.MAX_VALUE, Integer.MAX_VALUE, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new GraphMazeGenerationInput(317, 317, 1L));
        assertThrows(NullPointerException.class, () -> new ArrayMazeGenerator(null));
        assertThrows(NullPointerException.class, () -> new ArrayMazePathfinder(null));
    }

    @Test
    void publicInputsRejectExcessiveCollections() {
        assertThrows(IllegalArgumentException.class,
                () -> new IntegerSortInput(java.util.Collections.nCopies(
                        IntegerSortInput.MAX_VALUES + 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new IntGraph(java.util.Collections.nCopies(IntGraph.MAX_NODES + 1, 1), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new IntGraph(List.of(1), java.util.Collections.nCopies(
                        IntGraph.MAX_EDGES + 1, new IntEdge(1, 1))));
        assertThrows(IllegalArgumentException.class,
                () -> new AvlTreeInput(java.util.Collections.nCopies(
                        AvlTreeInput.MAX_OPERATIONS + 1, 1), List.of()));
    }

    @Test
    void arrayGeneratorsAreConnectedReproducibleAndReplayable() {
        for (String id : List.of(
                "maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find")) {
            InMemoryEventSink firstSink = new InMemoryEventSink();
            ArrayMazeGenerationOutput first = (ArrayMazeGenerationOutput) run(
                    id, new ArrayMazeGenerationInput(11, 13, 42L), firstSink);
            ArrayMazeGenerationOutput second = (ArrayMazeGenerationOutput) run(
                    id, new ArrayMazeGenerationInput(11, 13, 42L), new InMemoryEventSink());
            assertEquals(first, second, id);
            assertTrue(isReachable(first.maze(), first.maze().entrance(), first.maze().exit()), id);
            assertEquals(first.maze(), replayMaze(firstSink.events()), id);
        }
    }

    @Test
    void publicPathfindersReturnValidRoutesAndAStarIsShortest() {
        GridMaze maze = openMaze(5, 5, Set.of(new GridPoint(1, 2), new GridPoint(2, 2)));
        GridPoint start = new GridPoint(0, 0);
        GridPoint goal = new GridPoint(4, 4);
        ArrayMazePathOutput astar = (ArrayMazePathOutput) run(
                "maze-pathfinder-astar", new ArrayMazePathInput(maze, start, goal), new InMemoryEventSink());
        ArrayMazePathOutput dfs = (ArrayMazePathOutput) run(
                "maze-pathfinder-dfs", new ArrayMazePathInput(maze, start, goal), new InMemoryEventSink());
        assertValidPath(maze, astar.path(), start, goal);
        assertValidPath(maze, dfs.path(), start, goal);
        assertEquals(9, astar.path().size());
    }

    @Test
    void graphAlgorithmsAreCorrectAndEventsRebuildResults() {
        IntGraph graph = new IntGraph(
                List.of(0, 1, 2, 3, 4),
                List.of(new IntEdge(0, 1), new IntEdge(0, 2), new IntEdge(1, 3),
                        new IntEdge(2, 3), new IntEdge(3, 4)));
        InMemoryEventSink bfsSink = new InMemoryEventSink();
        GraphBfsOutput bfs = (GraphBfsOutput) run("graph-bfs", new GraphBfsInput(graph, 0), bfsSink);
        assertEquals(List.of(0, 1, 2, 3, 4), bfs.visitOrder());
        assertEquals(bfs.visitOrder(), bfsSink.events().stream()
                .map(ExecutionEvent::payload)
                .filter(GraphBfsEvent.Visited.class::isInstance)
                .map(GraphBfsEvent.Visited.class::cast)
                .map(GraphBfsEvent.Visited::node).toList());

        InMemoryEventSink mazeSink = new InMemoryEventSink();
        GraphMazeGenerationOutput generated = (GraphMazeGenerationOutput) run(
                "graph-generator-bfs", new GraphMazeGenerationInput(4, 5, 7L), mazeSink);
        assertEquals(20, generated.graph().nodes().size());
        assertEquals(38, generated.graph().edges().size());
        List<IntEdge> replayed = mazeSink.events().stream()
                .map(ExecutionEvent::payload)
                .filter(GraphMazeGenerationEvent.EdgeAdded.class::isInstance)
                .map(GraphMazeGenerationEvent.EdgeAdded.class::cast)
                .map(GraphMazeGenerationEvent.EdgeAdded::edge).toList();
        assertEquals(generated.graph().edges(), replayed);
    }

    @Test
    void avlCommandsPreserveOrderingBalanceAndReplayFinalSnapshot() {
        InMemoryEventSink sink = new InMemoryEventSink();
        AvlTreeOutput output = (AvlTreeOutput) run(
                "tree-avl",
                new AvlTreeInput(
                        List.of(30, 20, 10, 25, 40, 50),
                        List.of(
                                new AvlCommand(AvlCommand.Operation.REMOVE, 20),
                                new AvlCommand(AvlCommand.Operation.INSERT, 35))),
                sink);
        assertEquals(List.of(10, 25, 30, 35, 40, 50), output.values());
        assertAvl(output.root(), null, null);
        AvlTreeEvent.Completed completed = sink.events().stream()
                .map(ExecutionEvent::payload)
                .filter(AvlTreeEvent.Completed.class::isInstance)
                .map(AvlTreeEvent.Completed.class::cast)
                .findFirst().orElseThrow();
        assertEquals(output.root(), completed.root());
        assertEquals(output.values(), completed.values());
    }

    private Object run(String id, com.majortom.algorithms.core.api.AlgorithmInput input, InMemoryEventSink sink) {
        ExecutionResult result = new DefaultAlgorithmRunner().run(
                ProviderCatalog.production().require(id).invoker(), input, sink);
        assertEquals(ExecutionStatus.COMPLETED, result.status(), id);
        return result.output().orElseThrow();
    }

    private List<Integer> replaySort(List<ExecutionEvent> events) {
        List<Integer> values = new ArrayList<>();
        for (ExecutionEvent event : events) {
            if (event.payload() instanceof SortInitializedEvent initialized) {
                values = new ArrayList<>(initialized.values());
            } else if (event.payload() instanceof SortWrittenEvent written) {
                values.set(written.index(), written.value());
            } else if (event.payload() instanceof SortSwappedEvent swapped) {
                values.set(swapped.leftIndex(), swapped.leftValue());
                values.set(swapped.rightIndex(), swapped.rightValue());
            } else if (event.payload() instanceof SortCompletedEvent completed) {
                assertEquals(completed.values(), values);
            }
        }
        return values;
    }

    private GridMaze replayMaze(List<ExecutionEvent> events) {
        ArrayMazeGenerationEvent.Initialized initialized = events.stream()
                .map(ExecutionEvent::payload)
                .filter(ArrayMazeGenerationEvent.Initialized.class::isInstance)
                .map(ArrayMazeGenerationEvent.Initialized.class::cast).findFirst().orElseThrow();
        List<Boolean> open = new ArrayList<>();
        for (int index = 0; index < initialized.rows() * initialized.columns(); index++) {
            open.add(false);
        }
        for (ExecutionEvent event : events) {
            if (event.payload() instanceof ArrayMazeGenerationEvent.CellOpened cell) {
                open.set(cell.point().row() * initialized.columns() + cell.point().column(), true);
            }
        }
        return new GridMaze(initialized.rows(), initialized.columns(), open,
                initialized.entrance(), initialized.exit());
    }

    private boolean isReachable(GridMaze maze, GridPoint start, GridPoint goal) {
        ArrayDeque<GridPoint> queue = new ArrayDeque<>();
        Set<GridPoint> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            GridPoint point = queue.removeFirst();
            if (point.equals(goal)) {
                return true;
            }
            for (int[] direction : List.of(new int[]{-1, 0}, new int[]{0, 1},
                    new int[]{1, 0}, new int[]{0, -1})) {
                int row = point.row() + direction[0];
                int column = point.column() + direction[1];
                if (row < 0 || column < 0 || row >= maze.rows() || column >= maze.columns()) {
                    continue;
                }
                GridPoint next = new GridPoint(row, column);
                if (maze.isOpen(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    private GridMaze openMaze(int rows, int columns, Set<GridPoint> walls) {
        List<Boolean> open = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                open.add(!walls.contains(new GridPoint(row, column)));
            }
        }
        return new GridMaze(rows, columns, open, new GridPoint(0, 0), new GridPoint(rows - 1, columns - 1));
    }

    private void assertValidPath(GridMaze maze, List<GridPoint> path, GridPoint start, GridPoint goal) {
        assertFalse(path.isEmpty());
        assertEquals(start, path.getFirst());
        assertEquals(goal, path.getLast());
        for (int index = 0; index < path.size(); index++) {
            assertTrue(maze.isOpen(path.get(index)));
            if (index > 0) {
                GridPoint previous = path.get(index - 1);
                GridPoint current = path.get(index);
                assertEquals(1, Math.abs(previous.row() - current.row())
                        + Math.abs(previous.column() - current.column()));
            }
        }
    }

    private int assertAvl(AvlNodeSnapshot node, Integer lower, Integer upper) {
        if (node == null) {
            return 0;
        }
        if (lower != null) {
            assertTrue(node.value() > lower);
        }
        if (upper != null) {
            assertTrue(node.value() < upper);
        }
        int leftHeight = assertAvl(node.left(), lower, node.value());
        int rightHeight = assertAvl(node.right(), node.value(), upper);
        assertTrue(Math.abs(leftHeight - rightHeight) <= 1);
        assertEquals(Math.max(leftHeight, rightHeight) + 1, node.height());
        return node.height();
    }
}
