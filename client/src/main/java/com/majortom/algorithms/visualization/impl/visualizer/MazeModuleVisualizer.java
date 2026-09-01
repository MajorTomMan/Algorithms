package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.runtime.maze.MazeCellType;
import com.majortom.algorithms.library.graph.IntEdge;
import com.majortom.algorithms.library.maze.GridPoint;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import javafx.animation.AnimationTimer;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

/** Clan-terrain renderer for both array and graph maze projections. */
public final class MazeModuleVisualizer extends BaseVisualizer<MazeViewState> {

    private static final int MAX_TRAIL_SIZE = 6;
    private static final long FRAME_INTERVAL_NANOS = 33_000_000L;

    private final Deque<GridPoint> focusTrail = new ArrayDeque<>();
    private final AnimationTimer focusTimer;
    private GridPoint lastFocus;
    private long focusChangedAtNanos;
    private long lastFrameNanos;
    private boolean timerRunning;
    private boolean animationRequested = true;
    private int lastRows = -1;
    private int lastColumns = -1;
    private boolean lastGraphBased;

    public MazeModuleVisualizer() {
        focusTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrameNanos < FRAME_INTERVAL_NANOS) {
                    return;
                }
                lastFrameNanos = now;
                drawCurrent();
            }
        };
    }

    @Override
    protected void draw(MazeViewState state) {
        clear();
        if (state.rows() < 1 || state.columns() < 1
                || state.openCells().size() < state.rows() * state.columns()) {
            stopFocusTimer();
            return;
        }
        resetTrailForNewGrid(state);
        rememberFocus(state.focus());

        double cellWidth = canvas.getWidth() / state.columns();
        double cellHeight = canvas.getHeight() / state.rows();
        drawTerrain(state, cellWidth, cellHeight);
        if (state.graphBased()) {
            drawGraphEdges(state, cellWidth, cellHeight);
        }
        drawFocusTrail(state, cellWidth, cellHeight);
        drawFocus(state, cellWidth, cellHeight);
        drawTransientFeedbackOverlay();
        updateTimerState();
    }

    private void resetTrailForNewGrid(MazeViewState state) {
        if (state.rows() == lastRows && state.columns() == lastColumns && state.graphBased() == lastGraphBased) {
            return;
        }
        focusTrail.clear();
        lastFocus = null;
        lastRows = state.rows();
        lastColumns = state.columns();
        lastGraphBased = state.graphBased();
    }

    private void rememberFocus(GridPoint focus) {
        if (focus == null || focus.equals(lastFocus)) {
            return;
        }
        focusTrail.remove(focus);
        focusTrail.addLast(focus);
        while (focusTrail.size() > MAX_TRAIL_SIZE) {
            focusTrail.removeFirst();
        }
        lastFocus = focus;
        focusChangedAtNanos = System.nanoTime();
    }

    private void drawTerrain(MazeViewState state, double cellWidth, double cellHeight) {
        for (int row = 0; row < state.rows(); row++) {
            for (int column = 0; column < state.columns(); column++) {
                GridPoint point = new GridPoint(row, column);
                int index = row * state.columns() + column;
                int terrain = MazeCellType.WALL;
                if (state.openCells().get(index)) {
                    terrain = MazeCellType.ROAD;
                }
                if (point.equals(state.entrance())) {
                    terrain = MazeCellType.START;
                } else if (point.equals(state.exit())) {
                    terrain = MazeCellType.END;
                }
                renderTerrainCell(row, column, cellWidth, cellHeight,
                        colorFor(state, point, index), terrain);
            }
        }
    }

    private Color colorFor(MazeViewState state, GridPoint point, int index) {
        Color fill = RAN_WALL_STONE;
        if (state.openCells().get(index)) {
            fill = RAN_BLUE.deriveColor(0.0d, 1.2d, 1.0d, 0.45d);
        }
        if (state.discovered().contains(point)) {
            fill = RAN_DARK_BLUE.saturate();
        }
        if (state.visited().contains(point)) {
            fill = RAN_BLUE.saturate();
        }
        if (state.deadEnds().contains(point)) {
            fill = RAN_IRON;
        }
        if (state.backtracked().contains(point)) {
            fill = RAN_RED.saturate();
        }
        if (state.path().contains(point)) {
            fill = RAN_GOLD.saturate();
        }
        if (point.equals(state.entrance())) {
            fill = RAN_WHITE;
        }
        if (point.equals(state.exit())) {
            fill = RAN_VIOLET.saturate();
        }
        return fill;
    }

    private void renderTerrainCell(
            int row, int column, double width, double height, Color fill, int terrain) {
        double x = column * width;
        double y = row * height;
        double inset = Math.min(0.5d, Math.min(width, height) * 0.08d);
        Color monStroke = RAN_ENEMY_RUST.deriveColor(0.0d, 1.0d, 1.0d, 0.5d);

        if (terrain == MazeCellType.START) {
            fill = RAN_WHITE;
            monStroke = RAN_BLACK;
        } else if (terrain == MazeCellType.END) {
            fill = RAN_DARK_BLUE.saturate();
            monStroke = RAN_WHITE;
        } else if (terrain == MazeCellType.ROAD) {
            monStroke = RAN_BLUE.saturate();
        }

        gc.setFill(fill);
        gc.fillRect(x + inset, y + inset, Math.max(0.0d, width - inset * 2.0d),
                Math.max(0.0d, height - inset * 2.0d));

        double minimumCellSize = Math.min(width, height);
        if (minimumCellSize >= 7.0d) {
            double monSize = minimumCellSize * 0.52d;
            if (terrain != MazeCellType.WALL) {
                monSize *= 1.15d;
            }
            drawMazeClanMon(x + width / 2.0d, y + height / 2.0d, monSize, terrain, monStroke);
        }
    }

    private void drawMazeClanMon(double mx, double my, double size, int type, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(Math.max(1.2, size * 0.15));

        switch (type) {
            case MazeCellType.PATH -> gc.strokeOval(mx - size / 2, my - size / 2, size, size);
            case MazeCellType.ROAD -> gc.strokeLine(mx - size * 0.45, my, mx + size * 0.45, my);
            case MazeCellType.BACKTRACK, MazeCellType.DEADEND -> {
                double height = size * 0.866;
                gc.strokePolygon(
                        new double[] {mx, mx - size / 2, mx + size / 2},
                        new double[] {my - height / 2, my + height / 2, my + height / 2}, 3);
            }
            case MazeCellType.WALL -> {
                double offset = size * 0.35;
                gc.strokeLine(mx - offset, my - offset, mx + offset, my + offset);
                gc.strokeLine(mx + offset, my - offset, mx - offset, my + offset);
            }
            case MazeCellType.START, MazeCellType.END -> {
                gc.strokeOval(mx - size / 2, my - size / 2, size, size);
                gc.strokeOval(mx - size / 4, my - size / 4, size / 2, size / 2);
            }
            default -> gc.strokeOval(mx - size / 2, my - size / 2, size, size);
        }
    }

    private void drawGraphEdges(MazeViewState state, double cellWidth, double cellHeight) {
        gc.save();
        gc.setStroke(RAN_GOLD.deriveColor(0.0d, 1.0d, 1.0d, 0.72d));
        gc.setLineWidth(Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.18d));
        for (IntEdge edge : state.graphEdges()) {
            int fromRow = edge.from() / state.columns();
            int fromColumn = edge.from() % state.columns();
            int toRow = edge.to() / state.columns();
            int toColumn = edge.to() % state.columns();
            gc.strokeLine(
                    (fromColumn + 0.5d) * cellWidth,
                    (fromRow + 0.5d) * cellHeight,
                    (toColumn + 0.5d) * cellWidth,
                    (toRow + 0.5d) * cellHeight);
        }
        gc.restore();
    }

    private void drawFocusTrail(MazeViewState state, double cellWidth, double cellHeight) {
        if (state.focus() == null || Math.min(cellWidth, cellHeight) < 5.0d || focusTrail.size() < 2) {
            return;
        }
        int index = 0;
        int visibleTrailSize = focusTrail.size() - 1;
        for (GridPoint point : focusTrail) {
            if (point.equals(state.focus())) {
                continue;
            }
            double alpha = 0.28d + (double) index / Math.max(1, visibleTrailSize) * 0.42d;
            double x = point.column() * cellWidth;
            double y = point.row() * cellHeight;
            gc.save();
            gc.setGlobalAlpha(alpha);
            gc.setFill(RAN_CYAN.deriveColor(0.0d, 1.0d, 1.0d, 0.24d));
            gc.fillOval(x + cellWidth * 0.2d, y + cellHeight * 0.2d,
                    cellWidth * 0.6d, cellHeight * 0.6d);
            drawTrailFlag(x, y, cellWidth, cellHeight, point, index);
            gc.restore();
            index++;
        }
    }

    private void drawTrailFlag(
            double x, double y, double width, double height, GridPoint point, int index) {
        double poleX = x + width * 0.75d;
        double phase = System.nanoTime() * 0.000000004d + stableUnit(point, index) * Math.PI;
        double wave = Math.sin(phase) * width * 0.12d;
        gc.setStroke(RAN_IRON);
        gc.setLineWidth(Math.max(0.7d, Math.min(2.0d, width * 0.12d)));
        gc.strokeLine(poleX, y + height * 0.9d, poleX, y + height * 0.1d);
        gc.setFill(RAN_CYAN.interpolate(RAN_WHITE, 0.5d));
        gc.fillPolygon(
                new double[] {poleX, poleX - width * 0.5d + wave, poleX},
                new double[] {y + height * 0.1d, y + height * 0.4d, y + height * 0.7d}, 3);
        gc.setStroke(RAN_GOLD);
        gc.setLineWidth(Math.max(0.5d, Math.min(1.0d, width * 0.08d)));
        gc.strokePolygon(
                new double[] {poleX, poleX - width * 0.5d + wave, poleX},
                new double[] {y + height * 0.1d, y + height * 0.4d, y + height * 0.7d}, 3);
    }

    private void drawFocus(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint focus = state.focus();
        if (focus == null) {
            return;
        }
        double x = focus.column() * cellWidth;
        double y = focus.row() * cellHeight;
        double centerX = x + cellWidth / 2.0d;
        double centerY = y + cellHeight / 2.0d;
        double elapsedSeconds = (System.nanoTime() - focusChangedAtNanos) / 1_000_000_000.0d;
        double wobble = Math.sin(elapsedSeconds * 18.0d) * Math.min(1.5d, cellWidth * 0.08d);

        gc.save();
        applyFocusEffect();
        gc.setFill(RAN_CYAN.deriveColor(0.0d, 1.0d, 1.0d, 0.3d));
        gc.fillOval(centerX - cellWidth * 0.4d, centerY - cellHeight * 0.4d,
                cellWidth * 0.8d, cellHeight * 0.8d);
        drawKabuto(centerX + wobble, centerY, cellWidth, cellHeight, elapsedSeconds);
        if (elapsedSeconds < 0.65d && Math.min(cellWidth, cellHeight) >= 7.0d) {
            drawDeterministicSparks(focus, centerX, centerY, cellWidth, cellHeight, elapsedSeconds);
        }
        gc.setStroke(RAN_WHITE);
        gc.setLineWidth(Math.max(1.0d, Math.min(3.0d, Math.min(cellWidth, cellHeight) * 0.18d)));
        gc.strokeRect(x + 1.5d, y + 1.5d,
                Math.max(0.0d, cellWidth - 3.0d), Math.max(0.0d, cellHeight - 3.0d));
        releaseEffect();
        gc.restore();
    }

    private void drawKabuto(
            double centerX, double centerY, double width, double height, double elapsedSeconds) {
        double size = Math.min(width, height) * 0.85d;
        if (size < 4.0d) {
            return;
        }
        gc.setFill(RAN_BLACK.deriveColor(0.0d, 1.0d, 0.25d, 1.0d));
        gc.setStroke(RAN_IRON);
        gc.setLineWidth(Math.max(0.6d, size * 0.08d));
        gc.beginPath();
        gc.moveTo(centerX - size / 3.0d, centerY + size / 10.0d);
        gc.arc(centerX, centerY + size / 10.0d, size / 3.0d, size / 3.0d, 180.0d, 180.0d);
        gc.closePath();
        gc.fill();
        gc.stroke();

        gc.setFill(RAN_BLUE.deriveColor(0.0d, 1.2d, 0.85d, 0.9d));
        double plate = size / 4.0d;
        gc.fillPolygon(
                new double[] {centerX - size / 3.0d, centerX - plate, centerX - plate, centerX - size / 3.0d},
                new double[] {centerY - size / 10.0d, centerY - size / 10.0d,
                        centerY + size / 6.0d, centerY + size / 12.0d}, 4);
        gc.fillPolygon(
                new double[] {centerX + size / 3.0d, centerX + plate, centerX + plate, centerX + size / 3.0d},
                new double[] {centerY - size / 10.0d, centerY - size / 10.0d,
                        centerY + size / 6.0d, centerY + size / 12.0d}, 4);

        double hornAngle = 15.0d + Math.sin(elapsedSeconds * 8.0d) * 2.5d;
        double hornLength = size * 0.8d;
        double hornRadians = Math.toRadians(hornAngle);
        double leftX = centerX - Math.sin(hornRadians) * hornLength;
        double rightX = centerX + Math.sin(hornRadians) * hornLength;
        double hornY = centerY - size / 4.0d - Math.cos(hornRadians) * hornLength;
        gc.setStroke(RAN_WHITE);
        gc.setLineWidth(Math.max(0.8d, size * 0.12d));
        gc.strokeLine(centerX, centerY - size / 4.0d, leftX, hornY);
        gc.strokeLine(centerX, centerY - size / 4.0d, rightX, hornY);
        gc.setFill(RAN_GOLD);
        double sparkSize = Math.max(1.0d, size * 0.08d);
        gc.fillOval(leftX - sparkSize / 2.0d, hornY - sparkSize / 2.0d, sparkSize, sparkSize);
        gc.fillOval(rightX - sparkSize / 2.0d, hornY - sparkSize / 2.0d, sparkSize, sparkSize);
    }

    private void drawDeterministicSparks(
            GridPoint focus, double centerX, double centerY, double width, double height, double elapsedSeconds) {
        double intensity = 1.0d - elapsedSeconds / 0.65d;
        gc.setStroke(RAN_GOLD.deriveColor(0.0d, 1.0d, 1.0d, Math.max(0.0d, intensity)));
        gc.setLineWidth(Math.max(0.7d, Math.min(2.0d, width * 0.1d)));
        for (int index = 0; index < 6; index++) {
            double seed = stableUnit(focus, index);
            double angle = elapsedSeconds * 280.0d + index * 60.0d + seed * 25.0d;
            double radians = Math.toRadians(angle);
            double length = width * (0.34d + seed * 0.22d);
            gc.strokeLine(
                    centerX + Math.cos(radians) * width * 0.12d,
                    centerY + Math.sin(radians) * height * 0.12d,
                    centerX + Math.cos(radians) * length,
                    centerY + Math.sin(radians) * length);
        }
    }

    private double stableUnit(GridPoint point, int salt) {
        long value = point.row() * 73_856_093L ^ point.column() * 19_349_663L ^ salt * 83_492_791L;
        value ^= value >>> 13;
        return (value & 0xffffL) / 65535.0d;
    }

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        VisualizationActionType action = event.actionType();
        if (action == VisualizationActionType.EXECUTION_PAUSE) {
            animationRequested = false;
        } else if (action == VisualizationActionType.EXECUTION_RESUME
                || action == VisualizationActionType.EXECUTION_START
                || action == VisualizationActionType.MAZE_BUILD
                || action == VisualizationActionType.MAZE_SOLVE
                || action == VisualizationActionType.EXECUTION_RESET) {
            animationRequested = true;
        }
        if (action == VisualizationActionType.MAZE_BUILD || action == VisualizationActionType.MAZE_SOLVE) {
            focusTrail.clear();
            lastFocus = null;
            focusChangedAtNanos = 0L;
        }
        updateTimerState();
    }

    @Override
    public void onVisualizationReset() {
        resetLocalState();
        animationRequested = true;
        super.onVisualizationReset();
    }

    @Override
    public void onModuleAttached(String moduleId) {
        super.onModuleAttached(moduleId);
        updateTimerState();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        resetLocalState();
        super.onModuleDetached(moduleId);
        clear();
    }

    @Override
    protected void onResizeStateChanged(boolean resizing) {
        if (resizing) {
            stopFocusTimer();
            return;
        }
        updateTimerState();
    }

    @Override
    public void dispose() {
        resetLocalState();
        super.dispose();
    }

    private void updateTimerState() {
        MazeViewState state = currentState();
        boolean hasFocus = state != null && state.focus() != null;
        if (animationRequested && hasFocus && isModuleAttached() && !isResizeInProgress() && !isDisposed()) {
            startFocusTimer();
        } else {
            stopFocusTimer();
        }
    }

    private void startFocusTimer() {
        if (timerRunning) {
            return;
        }
        timerRunning = true;
        lastFrameNanos = 0L;
        focusTimer.start();
    }

    private void stopFocusTimer() {
        if (!timerRunning) {
            return;
        }
        focusTimer.stop();
        timerRunning = false;
    }

    private void resetLocalState() {
        stopFocusTimer();
        focusTrail.clear();
        lastFocus = null;
        focusChangedAtNanos = 0L;
        lastRows = -1;
        lastColumns = -1;
    }
}
