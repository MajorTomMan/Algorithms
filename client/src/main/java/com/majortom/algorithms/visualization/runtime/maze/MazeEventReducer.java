package com.majortom.algorithms.visualization.runtime.maze;

import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationEvent;
import com.majortom.algorithms.library.maze.ArrayMazePathEvent;
import com.majortom.algorithms.library.maze.GraphMazeGenerationEvent;
import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.library.maze.GridPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stateless reducer for array-maze generation/search and graph-maze generation. */
public final class MazeEventReducer implements EventReducer<MazeViewState> {

    private final int rows;
    private final int columns;
    private final boolean graphBased;

    public MazeEventReducer(int rows, int columns, boolean graphBased) {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("maze dimensions must be positive");
        }
        this.rows = rows;
        this.columns = columns;
        this.graphBased = graphBased;
    }

    @Override
    public MazeViewState initialState() {
        return MazeViewState.empty(rows, columns, graphBased);
    }

    @Override
    public Reduction<MazeViewState> reduce(MazeViewState previous, EventEnvelope event) {
        Object payload = event.event();
        if (payload instanceof ArrayMazeGenerationEvent.Initialized initialized) {
            MazeViewState state = MazeViewState.empty(initialized.rows(), initialized.columns(), false);
            state = copy(state, state.openCells(), Set.of(), Set.of(), Set.of(), Set.of(),
                    Map.of(), initialized.entrance(), initialized.exit(), initialized.entrance(),
                    List.of(), false, MazeViewState.Phase.GENERATING, false);
            return changed(state, EventImportance.CHECKPOINT);
        }
        if (payload instanceof ArrayMazeGenerationEvent.CellOpened opened) {
            List<Boolean> cells = new ArrayList<>(previous.openCells());
            cells.set(index(previous, opened.point()), true);
            return changed(copy(previous, cells, previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), previous.parents(), previous.entrance(),
                    previous.exit(), opened.point(), previous.graphEdges(), false,
                    MazeViewState.Phase.GENERATING, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof ArrayMazeGenerationEvent.Completed completed) {
            GridMaze maze = completed.maze();
            return changed(copy(previous, maze.openCells(), previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), previous.parents(), maze.entrance(),
                    maze.exit(), previous.focus(), previous.graphEdges(), false,
                    MazeViewState.Phase.COMPLETED, true), EventImportance.CHECKPOINT);
        }
        if (payload instanceof ArrayMazePathEvent.Initialized initialized) {
            GridMaze maze = initialized.maze();
            MazeViewState state = new MazeViewState(maze.rows(), maze.columns(), maze.openCells(),
                    Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Map.of(), initialized.start(),
                    initialized.goal(), initialized.start(), List.of(), false,
                    MazeViewState.Phase.SEARCHING, false);
            return changed(state, EventImportance.CHECKPOINT);
        }
        if (payload instanceof ArrayMazePathEvent.Discovered discovered) {
            Set<GridPoint> cells = plus(previous.discovered(), discovered.point());
            Map<GridPoint, GridPoint> parents = parent(previous.parents(), discovered.point(), discovered.parent());
            return changed(copy(previous, previous.openCells(), cells, previous.visited(),
                    previous.deadEnds(), previous.backtracked(), previous.path(), parents,
                    discovered.point(), MazeViewState.Phase.DISCOVERING, false),
                    EventImportance.TRANSIENT);
        }
        if (payload instanceof ArrayMazePathEvent.Entered entered) {
            Set<GridPoint> discovered = plus(previous.discovered(), entered.point());
            Set<GridPoint> visited = plus(previous.visited(), entered.point());
            Map<GridPoint, GridPoint> parents = parent(previous.parents(), entered.point(), entered.parent());
            return changed(copy(previous, previous.openCells(), discovered, visited,
                    previous.deadEnds(), previous.backtracked(), previous.path(), parents,
                    entered.point(), MazeViewState.Phase.ENTERING, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof ArrayMazePathEvent.DeadEndReached deadEnd) {
            return changed(copy(previous, previous.openCells(), previous.discovered(), previous.visited(),
                    plus(previous.deadEnds(), deadEnd.point()), previous.backtracked(), previous.path(),
                    previous.parents(), deadEnd.point(), MazeViewState.Phase.DEAD_END, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof ArrayMazePathEvent.Backtracked backtracked) {
            return changed(copy(previous, previous.openCells(), previous.discovered(), previous.visited(),
                    previous.deadEnds(), plus(previous.backtracked(), backtracked.from()), previous.path(),
                    previous.parents(), backtracked.to(), MazeViewState.Phase.BACKTRACKING, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof ArrayMazePathEvent.PathConfirmed confirmed) {
            return changed(copy(previous, previous.openCells(), previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), plus(previous.path(), confirmed.point()),
                    previous.parents(), confirmed.point(), MazeViewState.Phase.CONFIRMING_PATH, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof ArrayMazePathEvent.Completed completed) {
            Set<GridPoint> path = new LinkedHashSet<>(previous.path());
            path.addAll(completed.path());
            GridPoint focus = previous.focus();
            if (!completed.path().isEmpty()) {
                focus = completed.path().getLast();
            }
            return changed(copy(previous, previous.openCells(), previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), path, previous.parents(), focus,
                    MazeViewState.Phase.COMPLETED, true), EventImportance.TERMINAL);
        }
        if (payload instanceof GraphMazeGenerationEvent.Initialized initialized) {
            MazeViewState state = MazeViewState.empty(initialized.rows(), initialized.columns(), true);
            return changed(copy(state, openGraphCells(state), Set.of(), Set.of(), Set.of(), Set.of(),
                    Map.of(), null, null, null, List.of(), true, MazeViewState.Phase.GENERATING, false),
                    EventImportance.CHECKPOINT);
        }
        if (payload instanceof GraphMazeGenerationEvent.EdgeAdded added) {
            List<IntEdge> edges = new ArrayList<>(previous.graphEdges());
            edges.add(added.edge());
            return changed(copy(previous, openGraphCells(previous), previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), previous.parents(), null, null,
                    point(previous, added.edge().to()), edges, true, MazeViewState.Phase.GENERATING, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof GraphMazeGenerationEvent.Completed completed) {
            return changed(copy(previous, openGraphCells(previous), previous.discovered(), previous.visited(),
                    previous.deadEnds(), previous.backtracked(), previous.parents(), null, null,
                    previous.focus(), completed.graph().edges(), true, MazeViewState.Phase.COMPLETED, true),
                    EventImportance.TERMINAL);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static MazeViewState copy(
            MazeViewState base, List<Boolean> open, Set<GridPoint> discovered, Set<GridPoint> visited,
            Set<GridPoint> deadEnds, Set<GridPoint> backtracked, Set<GridPoint> path,
            Map<GridPoint, GridPoint> parents, GridPoint focus, MazeViewState.Phase phase,
            boolean completed) {
        return copy(base, open, discovered, visited, deadEnds, backtracked, path, parents,
                base.entrance(), base.exit(), focus, base.graphEdges(), base.graphBased(), phase, completed);
    }

    private static MazeViewState copy(
            MazeViewState base, List<Boolean> open, Set<GridPoint> discovered, Set<GridPoint> visited,
            Set<GridPoint> deadEnds, Set<GridPoint> backtracked, Map<GridPoint, GridPoint> parents,
            GridPoint entrance, GridPoint exit, GridPoint focus, List<IntEdge> edges,
            boolean graphBased, MazeViewState.Phase phase, boolean completed) {
        return copy(base, open, discovered, visited, deadEnds, backtracked, base.path(), parents,
                entrance, exit, focus, edges, graphBased, phase, completed);
    }

    private static MazeViewState copy(
            MazeViewState base, List<Boolean> open, Set<GridPoint> discovered, Set<GridPoint> visited,
            Set<GridPoint> deadEnds, Set<GridPoint> backtracked, Set<GridPoint> path,
            Map<GridPoint, GridPoint> parents, GridPoint entrance, GridPoint exit, GridPoint focus,
            List<IntEdge> edges, boolean graphBased, MazeViewState.Phase phase, boolean completed) {
        return new MazeViewState(base.rows(), base.columns(), open, discovered, visited, deadEnds,
                backtracked, path, parents, entrance, exit, focus, edges, graphBased, phase, completed);
    }

    private static int index(MazeViewState state, GridPoint point) {
        return point.row() * state.columns() + point.column();
    }

    private static GridPoint point(MazeViewState state, int node) {
        return new GridPoint(node / state.columns(), node % state.columns());
    }

    private static List<Boolean> openGraphCells(MazeViewState state) {
        return Collections.nCopies(state.rows() * state.columns(), true);
    }

    private static Set<GridPoint> plus(Set<GridPoint> source, GridPoint point) {
        if (point == null) {
            return source;
        }
        Set<GridPoint> result = new LinkedHashSet<>(source);
        result.add(point);
        return result;
    }

    private static Map<GridPoint, GridPoint> parent(
            Map<GridPoint, GridPoint> source, GridPoint point, GridPoint parent) {
        if (parent == null) {
            return source;
        }
        Map<GridPoint, GridPoint> result = new LinkedHashMap<>(source);
        result.put(point, parent);
        return result;
    }

    private static Reduction<MazeViewState> changed(MazeViewState state, EventImportance importance) {
        return Reduction.changed(state, importance, true);
    }
}
