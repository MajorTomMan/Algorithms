package com.majortom.algorithms.visualization.runtime.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.majortom.algorithms.algorithm.maze.generator.impl.BfsArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.generator.impl.DfsArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.generator.impl.UnionFindArrayMazeGenerator;
import com.majortom.algorithms.algorithm.maze.pathfinder.impl.AStarArrayMazePathfinder;
import com.majortom.algorithms.algorithm.maze.pathfinder.impl.DfsArrayMazePathfinder;
import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.structure.maze.GridMaze;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.structure.maze.Maze;
import com.majortom.algorithms.structure.maze.MazeDimensions;
import com.majortom.algorithms.structure.maze.MazeEndpointPolicy;
import com.majortom.algorithms.structure.maze.MazeEndpoints;
import com.majortom.algorithms.structure.maze.MazeStructure;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class MazeEndpointPolicyTest {
  @Test
  void selectsDifferentInternalOddLogicalCells() {
    MazeEndpoints endpoints = MazeEndpointPolicy.random().resolve(new MazeDimensions(11, 13),
        new Random(7));

    assertNotEquals(endpoints.entrance(), endpoints.exit());
    assertTrue(isLogicalCell(endpoints.entrance(), 11, 13));
    assertTrue(isLogicalCell(endpoints.exit(), 11, 13));
  }

  @Test
  void allowsTheOnlyLogicalCellToServeAsBothEndpoints() {
    MazeEndpoints endpoints = MazeEndpointPolicy.random().resolve(new MazeDimensions(3, 3),
        new Random(7));

    assertEquals(new GridPoint(1, 1), endpoints.entrance());
    assertEquals(endpoints.entrance(), endpoints.exit());
  }

  @Test
  void fixedPolicyUsesTheExactLegacyEndpoints() {
    MazeDimensions dimensions = new MazeDimensions(9, 13);
    MazeEndpoints expected = new MazeEndpoints(new GridPoint(1, 1), new GridPoint(7, 11));
    Maze maze = new Maze(dimensions, MazeEndpointPolicy.fixed());

    assertEquals(expected, maze.generationEndpoints());
    GridMaze generated = new DfsArrayMazeGenerator().generate(maze);
    assertEquals(expected.entrance(), generated.entrance());
    assertEquals(expected.exit(), generated.exit());
  }

  @Test
  void structureDefaultKeepsFixedEndpointsForExternalImplementations() {
    MazeDimensions dimensions = new MazeDimensions(7, 9);
    MazeStructure structure = new MazeStructure() {
      @Override
      public MazeDimensions dimensions() {
        return dimensions;
      }

      @Override
      public GridMaze grid() {
        return null;
      }

      @Override
      public void initialize(MazeDimensions ignored) {}

      @Override
      public void initialize(GridMaze ignored) {}
    };

    assertEquals(new MazeEndpoints(new GridPoint(1, 1), new GridPoint(5, 7)),
        structure.generationEndpoints());
  }

  @Test
  void rejectsCustomCoordinatesOutsideLogicalCells() {
    MazeEndpointPolicy evenCoordinate = MazeEndpointPolicy.custom(
        new GridPoint(2, 1), new GridPoint(7, 7));
    MazeEndpointPolicy boundaryCoordinate = MazeEndpointPolicy.custom(
        new GridPoint(1, 1), new GridPoint(11, 7));

    assertThrows(IllegalArgumentException.class,
        () -> evenCoordinate.resolve(new MazeDimensions(11, 11)));
    assertThrows(IllegalArgumentException.class,
        () -> boundaryCoordinate.resolve(new MazeDimensions(11, 11)));
  }

  @Test
  void randomMazeResolvesEndpointsOnceAndSharesThemWithTheGenerator() {
    Maze maze = new Maze(new MazeDimensions(15, 15), MazeEndpointPolicy.random());

    MazeEndpoints first = maze.generationEndpoints();
    MazeEndpoints second = maze.generationEndpoints();
    GridMaze generated = new BfsArrayMazeGenerator().generate(maze);

    assertSame(first, second);
    assertEquals(first.entrance(), generated.entrance());
    assertEquals(first.exit(), generated.exit());
  }

  @Test
  void reinitializingMazeClearsTheResolvedRandomEndpoints() {
    Maze maze = new Maze(new MazeDimensions(3, 3), MazeEndpointPolicy.random());

    MazeEndpoints first = maze.generationEndpoints();
    maze.initialize(new MazeDimensions(15, 15));
    MazeEndpoints second = maze.generationEndpoints();

    assertNotSame(first, second);
    assertTrue(isLogicalCell(second.entrance(), 15, 15));
    assertTrue(isLogicalCell(second.exit(), 15, 15));
  }

  @Test
  void legacyGenerationFactoriesRemainDeterministic() {
    MazeViewState firstReducerState = new MazeEventReducer(11, 11, false).initialState();
    MazeViewState secondReducerState = new MazeEventReducer(11, 11, false).initialState();
    MazeViewState firstFactoryState = MazeViewState.generation(11, 11, false);
    MazeViewState secondFactoryState = MazeViewState.generation(11, 11, false);

    assertEquals(firstReducerState, secondReducerState);
    assertEquals(firstFactoryState, secondFactoryState);
    assertEquals(new GridPoint(1, 1), firstFactoryState.entrance());
    assertEquals(new GridPoint(9, 9), firstFactoryState.exit());
  }

  @Test
  void explicitEndpointInjectionIsDeterministicAcrossReducerInstances() {
    MazeEndpoints endpoints = new MazeEndpoints(new GridPoint(3, 7), new GridPoint(9, 1));

    MazeViewState first = new MazeEventReducer(11, 11, false, endpoints).initialState();
    MazeViewState second = new MazeEventReducer(11, 11, false, endpoints).initialState();

    assertEquals(first, second);
    assertEndpoints(first, endpoints);
  }

  @Test
  void allArrayGeneratorsKeepSelectedEndpointsOpenAndReachable() {
    MazeDimensions dimensions = new MazeDimensions(21, 21);
    MazeEndpoints endpoints = MazeEndpointPolicy.random().resolve(dimensions, new Random(11));
    MazeEndpointPolicy policy = MazeEndpointPolicy.custom(endpoints.entrance(), endpoints.exit());
    List<Supplier<GridMaze>> generators = List.of(
        () -> new DfsArrayMazeGenerator().generate(new Maze(dimensions, policy)),
        () -> new BfsArrayMazeGenerator().generate(new Maze(dimensions, policy)),
        () -> new UnionFindArrayMazeGenerator().generate(new Maze(dimensions, policy)));

    for (Supplier<GridMaze> generator : generators) {
      GridMaze generated = generator.get();

      assertTrue(generated.isOpen(endpoints.entrance()));
      assertTrue(generated.isOpen(endpoints.exit()));
      assertTrue(isReachable(generated, endpoints.entrance(), endpoints.exit()));
    }
  }

  @Test
  void replayStateAndGeneratedGridShareTheSameEndpointPair() {
    MazeEndpoints endpoints = MazeEndpointPolicy.random().resolve(new MazeDimensions(9, 9),
        new Random(13));
    MazeEventReducer reducer = new MazeEventReducer(9, 9, false, endpoints);
    MazeViewState initial = reducer.initialState();

    assertEndpoints(initial, endpoints);
    MazeViewState updated = reducer.reduce(initial,
        envelope(1, new AlgorithmEvent.Visited(
            new AlgorithmEvent.CoordinateRef(endpoints.entrance().row(), endpoints.entrance().column()))))
        .state();
    assertEndpoints(updated, endpoints);

    Maze input = new Maze(new MazeDimensions(9, 9),
        MazeEndpointPolicy.custom(endpoints.entrance(), endpoints.exit()));
    GridMaze generated = new DfsArrayMazeGenerator().generate(input);
    assertEquals(endpoints.entrance(), generated.entrance());
    assertEquals(endpoints.exit(), generated.exit());
  }

  @Test
  void pathfindersReadTheSelectedEndpointsFromTheGeneratedGrid() {
    MazeDimensions dimensions = new MazeDimensions(15, 15);
    MazeEndpoints endpoints = MazeEndpointPolicy.random().resolve(dimensions, new Random(17));
    Maze input = new Maze(dimensions,
        MazeEndpointPolicy.custom(endpoints.entrance(), endpoints.exit()));
    GridMaze generated = new BfsArrayMazeGenerator().generate(input);

    List<GridPoint> dfsPath = new DfsArrayMazePathfinder().findPath(new Maze(generated));
    List<GridPoint> aStarPath = new AStarArrayMazePathfinder().findPath(new Maze(generated));
    assertEquals(endpoints.entrance(), dfsPath.getFirst());
    assertEquals(endpoints.exit(), dfsPath.getLast());
    assertEquals(endpoints.entrance(), aStarPath.getFirst());
    assertEquals(endpoints.exit(), aStarPath.getLast());
  }

  @Test
  void graphGenerationRetainsNoEndpointSemantics() {
    MazeViewState initial = new MazeEventReducer(9, 9, true).initialState();

    assertNull(initial.entrance());
    assertNull(initial.exit());
  }

  private static void assertEndpoints(MazeViewState state, MazeEndpoints endpoints) {
    assertEquals(endpoints.entrance(), state.entrance());
    assertEquals(endpoints.exit(), state.exit());
  }

  private static boolean isLogicalCell(GridPoint point, int rows, int columns) {
    return point.row() > 0 && point.row() < rows - 1
        && point.column() > 0 && point.column() < columns - 1
        && point.row() % 2 == 1 && point.column() % 2 == 1;
  }

  private static boolean isReachable(GridMaze maze, GridPoint start, GridPoint goal) {
    ArrayDeque<GridPoint> frontier = new ArrayDeque<>();
    Set<GridPoint> discovered = new HashSet<>();
    frontier.add(start);
    discovered.add(start);
    int[][] directions = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
    while (!frontier.isEmpty()) {
      GridPoint current = frontier.removeFirst();
      if (current.equals(goal)) {
        return true;
      }
      for (int[] direction : directions) {
        int row = current.row() + direction[0];
        int column = current.column() + direction[1];
        if (row < 0 || column < 0 || row >= maze.rows() || column >= maze.columns()) {
          continue;
        }
        GridPoint next = new GridPoint(row, column);
        if (maze.isOpen(next) && discovered.add(next)) {
          frontier.addLast(next);
        }
      }
    }
    return false;
  }

  private static EventEnvelope envelope(long sequence, ExecutionEvent event) {
    return new EventEnvelope("test-run", "maze-test", sequence,
        Instant.EPOCH.plusMillis(sequence), "test", event);
  }
}
