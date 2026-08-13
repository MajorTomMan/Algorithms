package com.majortom.algorithms.library.maze;

import java.util.List;
import java.util.Objects;

/** Immutable row-major binary maze; {@code true} denotes a traversable cell. */
public record GridMaze(
        int rows,
        int columns,
        List<Boolean> openCells,
        GridPoint entrance,
        GridPoint exit) {

    public GridMaze {
        if (rows < 1 || columns < 1) {
            throw new IllegalArgumentException("maze dimensions must be positive");
        }
        int cellCount = MazeDimensions.checkedCellCount(rows, columns);
        Objects.requireNonNull(openCells, "openCells");
        openCells = List.copyOf(openCells);
        if (openCells.size() != cellCount) {
            throw new IllegalArgumentException("openCells size does not match maze dimensions");
        }
        entrance = requireInside(Objects.requireNonNull(entrance, "entrance"), rows, columns, "entrance");
        exit = requireInside(Objects.requireNonNull(exit, "exit"), rows, columns, "exit");
        if (!openCells.get(index(columns, entrance)) || !openCells.get(index(columns, exit))) {
            throw new IllegalArgumentException("entrance and exit must be open");
        }
    }

    public boolean isOpen(GridPoint point) {
        requireInside(point, rows, columns, "point");
        return openCells.get(index(columns, point));
    }

    private static int index(int columns, GridPoint point) {
        return point.row() * columns + point.column();
    }

    private static GridPoint requireInside(GridPoint point, int rows, int columns, String name) {
        if (point.row() >= rows || point.column() >= columns) {
            throw new IllegalArgumentException(name + " must be inside the maze");
        }
        return point;
    }
}
