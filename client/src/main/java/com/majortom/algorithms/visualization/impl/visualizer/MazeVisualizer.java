package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.library.maze.GridPoint;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.runtime.maze.MazeCellType;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import javafx.scene.paint.Color;

/** Project-owned Canvas/Grid maze renderer hosted by the shared GestureFX visualization surface. */
public final class MazeVisualizer extends BaseVisualizer<MazeViewState> {
    private final VisualizationSurface surface = new VisualizationSurface();

    public MazeVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.nodeLayer().getChildren().add(canvas);
        surface.markViewportPristine();
    }

    @Override
    protected void draw(MazeViewState state) {
        clear();
        if (state.rows() < 1 || state.columns() < 1
                || state.openCells().size() < state.rows() * state.columns()) {
            surface.fitIfPristine();
            return;
        }
        double cellWidth = canvas.getWidth() / state.columns();
        double cellHeight = canvas.getHeight() / state.rows();
        drawTerrain(state, cellWidth, cellHeight);
        if (state.graphBased()) {
            drawGraphEdges(state, cellWidth, cellHeight);
        }
        surface.fitIfPristine();
    }

    private void drawTerrain(MazeViewState state, double cellWidth, double cellHeight) {
        for (int row = 0; row < state.rows(); row++) {
            for (int column = 0; column < state.columns(); column++) {
                GridPoint point = new GridPoint(row, column);
                int index = row * state.columns() + column;
                int terrain = state.openCells().get(index) ? MazeCellType.ROAD : MazeCellType.WALL;
                if (point.equals(state.entrance())) {
                    terrain = MazeCellType.START;
                } else if (point.equals(state.exit())) {
                    terrain = MazeCellType.END;
                } else if (state.path().contains(point)) {
                    terrain = MazeCellType.PATH;
                }
                renderTerrainCell(row, column, cellWidth, cellHeight, colorFor(state, point, index), terrain);
            }
        }
    }

    private Color colorFor(MazeViewState state, GridPoint point, int index) {
        if (point.equals(state.entrance())) {
            return RAN_WHITE;
        }
        if (point.equals(state.exit())) {
            return RAN_VIOLET.saturate();
        }
        if (state.path().contains(point)) {
            return RAN_GOLD.saturate();
        }
        if (state.openCells().get(index)) {
            return RAN_BLUE.deriveColor(0.0d, 1.2d, 1.0d, 0.45d);
        }
        return RAN_WALL_STONE;
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
        } else if (terrain == MazeCellType.ROAD || terrain == MazeCellType.PATH) {
            monStroke = terrain == MazeCellType.PATH ? RAN_GOLD : RAN_BLUE.saturate();
        }

        gc.setFill(fill);
        gc.fillRect(
                x + inset,
                y + inset,
                Math.max(0.0d, width - inset * 2.0d),
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

    private void drawMazeClanMon(double x, double y, double size, int type, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(Math.max(1.2d, size * 0.15d));
        if (type == MazeCellType.PATH) {
            gc.strokeOval(x - size / 2.0d, y - size / 2.0d, size, size);
            return;
        }
        if (type == MazeCellType.ROAD) {
            gc.strokeLine(x - size * 0.45d, y, x + size * 0.45d, y);
            return;
        }
        if (type == MazeCellType.WALL) {
            double offset = size * 0.35d;
            gc.strokeLine(x - offset, y - offset, x + offset, y + offset);
            gc.strokeLine(x + offset, y - offset, x - offset, y + offset);
            return;
        }
        gc.strokeOval(x - size / 2.0d, y - size / 2.0d, size, size);
        if (type == MazeCellType.START || type == MazeCellType.END) {
            gc.strokeOval(x - size / 4.0d, y - size / 4.0d, size / 2.0d, size / 2.0d);
        }
    }

    private void drawGraphEdges(MazeViewState state, double cellWidth, double cellHeight) {
        gc.save();
        gc.setStroke(RAN_GOLD.deriveColor(0.0d, 1.0d, 1.0d, 0.72d));
        gc.setLineWidth(Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.18d));
        for (MazeSnapshot.Edge edge : state.graphEdges()) {
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

    @Override
    public void onVisualizationReset() {
        clear();
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }
}
