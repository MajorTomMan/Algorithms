package com.majortom.algorithms.visualization.runtime.maze;

/** Visual terrain states used only by the JavaFX maze renderer. */
public final class MazeCellType {

    public static final int ROAD = 0;
    public static final int WALL = 1;
    public static final int PATH = 2;
    public static final int START = 3;
    public static final int DEADEND = 4;
    public static final int END = 5;
    public static final int BACKTRACK = 6;

    private MazeCellType() {
    }
}
