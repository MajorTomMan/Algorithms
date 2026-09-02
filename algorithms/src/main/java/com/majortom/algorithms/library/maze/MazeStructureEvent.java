package com.majortom.algorithms.library.maze;

import com.majortom.algorithms.core.event.ExecutionEvent;

/** Semantic mutations of the editable maze structure. */
public sealed interface MazeStructureEvent extends ExecutionEvent permits MazeStructureEvent.ResultApplied {
    record ResultApplied(int rows, int columns, boolean graphBased) implements MazeStructureEvent {}
}
