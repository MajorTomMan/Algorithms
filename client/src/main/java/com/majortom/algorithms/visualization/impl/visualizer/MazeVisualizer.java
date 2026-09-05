package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.library.maze.GridPoint;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Project-owned Canvas/Grid maze renderer hosted by the shared GestureFX visualization surface. */
public final class MazeVisualizer extends BaseVisualizer<MazeViewState> {
    private static final Color WALL_FILL = Color.web("#444444");
    private static final Color GRID_STROKE = Color.web("#D6D6D6");
    private static final Color GRID_STROKE_COMPACT = Color.web("#E5E5E5");
    private static final double DENSE_CELL_THRESHOLD = 8.0d;
    private static final double COMPACT_CELL_THRESHOLD = 14.0d;

    private final VisualizationSurface surface = new VisualizationSurface();
    private Consumer<GridPoint> selectionListener = ignored -> { };
    private GridPoint selectedCell;
    private VisualDensity density = VisualDensity.DETAIL;

    public MazeVisualizer() {
        getChildren().setAll(surface);
        surface.prefWidthProperty().bind(widthProperty());
        surface.prefHeightProperty().bind(heightProperty());
        surface.nodeLayer().getChildren().add(canvas);
        canvas.setOnMouseClicked(event -> {
            MazeViewState state = currentState();
            if (state == null || state.rows() < 1 || state.columns() < 1) return;
            double cellWidth = canvas.getWidth() / state.columns();
            double cellHeight = canvas.getHeight() / state.rows();
            int column = (int) Math.floor(event.getX() / cellWidth);
            int row = (int) Math.floor(event.getY() / cellHeight);
            if (row < 0 || row >= state.rows() || column < 0 || column >= state.columns()) return;
            selectedCell = new GridPoint(row, column);
            selectionListener.accept(selectedCell);
            requestRender();
            event.consume();
        });
        surface.markViewportPristine();
    }

    @Override
    protected void draw(MazeViewState state) {
        fillBackground();
        if (state.rows() < 1 || state.columns() < 1
                || state.openCells().size() < state.rows() * state.columns()) {
            surface.fitIfPristine();
            return;
        }

        double cellWidth = canvas.getWidth() / state.columns();
        double cellHeight = canvas.getHeight() / state.rows();
        double cellSize = Math.min(cellWidth, cellHeight);
        density = densityFor(cellSize);

        drawBaseGrid(state, cellWidth, cellHeight);
        if (state.graphBased()) {
            drawGraphEdges(state, cellWidth, cellHeight);
        }
        drawVisited(state, cellWidth, cellHeight);
        drawObserved(state, cellWidth, cellHeight);
        drawBacktracked(state, cellWidth, cellHeight);
        drawPath(state, cellWidth, cellHeight);
        drawCurrent(state, cellWidth, cellHeight);
        drawRoles(state, cellWidth, cellHeight);
        drawSelection(state, cellWidth, cellHeight);
        surface.fitIfPristine();
    }

    private void fillBackground() {
        gc.setEffect(null);
        gc.setFill(RAN_WHITE);
        gc.fillRect(0.0d, 0.0d, canvas.getWidth(), canvas.getHeight());
    }

    private void drawBaseGrid(MazeViewState state, double cellWidth, double cellHeight) {
        if (density == VisualDensity.DENSE) {
            drawDenseBase(state, cellWidth, cellHeight);
            return;
        }

        gc.setLineWidth(1.0d);
        gc.setStroke(density == VisualDensity.DETAIL ? GRID_STROKE : GRID_STROKE_COMPACT);
        for (int row = 0; row < state.rows(); row++) {
            for (int column = 0; column < state.columns(); column++) {
                int index = row * state.columns() + column;
                double x = column * cellWidth;
                double y = row * cellHeight;
                gc.setFill(state.openCells().get(index) ? RAN_WHITE : WALL_FILL);
                gc.fillRect(x, y, cellWidth, cellHeight);
                gc.strokeRect(x + 0.5d, y + 0.5d,
                        Math.max(0.0d, cellWidth - 1.0d),
                        Math.max(0.0d, cellHeight - 1.0d));
            }
        }
    }

    private void drawDenseBase(MazeViewState state, double cellWidth, double cellHeight) {
        gc.setFill(WALL_FILL);
        gc.fillRect(0.0d, 0.0d, canvas.getWidth(), canvas.getHeight());
        gc.setFill(RAN_WHITE);
        for (int row = 0; row < state.rows(); row++) {
            for (int column = 0; column < state.columns(); column++) {
                int index = row * state.columns() + column;
                if (!state.openCells().get(index)) continue;
                double x0 = Math.floor(column * cellWidth);
                double y0 = Math.floor(row * cellHeight);
                double x1 = Math.ceil((column + 1) * cellWidth);
                double y1 = Math.ceil((row + 1) * cellHeight);
                gc.fillRect(x0, y0, Math.max(1.0d, x1 - x0), Math.max(1.0d, y1 - y0));
            }
        }
    }

    private void drawGraphEdges(MazeViewState state, double cellWidth, double cellHeight) {
        gc.save();
        gc.setStroke(RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.68d));
        gc.setLineWidth(Math.max(1.2d, Math.min(2.2d, Math.min(cellWidth, cellHeight) * 0.06d)));
        Set<String> drawn = new HashSet<>();
        for (MazeSnapshot.Edge edge : state.graphEdges()) {
            int from = edge.from();
            int to = edge.to();
            String key = Math.min(from, to) + ":" + Math.max(from, to);
            if (!drawn.add(key)) continue;
            int fromRow = from / state.columns();
            int fromColumn = from % state.columns();
            int toRow = to / state.columns();
            int toColumn = to % state.columns();
            gc.strokeLine(
                    (fromColumn + 0.5d) * cellWidth,
                    (fromRow + 0.5d) * cellHeight,
                    (toColumn + 0.5d) * cellWidth,
                    (toRow + 0.5d) * cellHeight);
        }
        gc.restore();
    }

    private void drawVisited(MazeViewState state, double cellWidth, double cellHeight) {
        gc.setFill(RAN_BLUE);
        double inset = density == VisualDensity.DENSE ? 0.0d : Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.12d);
        for (GridPoint point : state.visited()) {
            if (!inside(state, point)) continue;
            double x = point.column() * cellWidth;
            double y = point.row() * cellHeight;
            gc.fillRect(x + inset, y + inset,
                    Math.max(0.0d, cellWidth - inset * 2.0d),
                    Math.max(0.0d, cellHeight - inset * 2.0d));
        }
    }

    private void drawObserved(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.observed();
        if (!inside(state, point)) return;
        double inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.16d);
        gc.setFill(RAN_YELLOW);
        gc.fillRect(point.column() * cellWidth + inset, point.row() * cellHeight + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
    }

    private void drawBacktracked(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.backtracked();
        if (!inside(state, point)) return;
        double inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.18d);
        gc.setFill(RAN_YELLOW);
        gc.fillRect(point.column() * cellWidth + inset, point.row() * cellHeight + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
    }

    private void drawPath(MazeViewState state, double cellWidth, double cellHeight) {
        if (state.path().isEmpty()) return;
        gc.save();
        gc.setStroke(RAN_YELLOW);
        gc.setFill(RAN_YELLOW);
        double lineWidth = Math.max(2.0d, Math.min(cellWidth, cellHeight) * 0.24d);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (GridPoint point : state.path()) {
            if (!inside(state, point)) continue;
            double x = (point.column() + 0.5d) * cellWidth;
            double y = (point.row() + 0.5d) * cellHeight;
            GridPoint right = new GridPoint(point.row(), point.column() + 1);
            GridPoint down = new GridPoint(point.row() + 1, point.column());
            if (state.path().contains(right)) {
                gc.strokeLine(x, y, x + cellWidth, y);
            }
            if (state.path().contains(down)) {
                gc.strokeLine(x, y, x, y + cellHeight);
            }
            gc.fillOval(x - lineWidth / 2.0d, y - lineWidth / 2.0d, lineWidth, lineWidth);
        }
        gc.restore();
    }

    private void drawCurrent(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.active();
        if (!inside(state, point)) return;
        double inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.10d);
        gc.setStroke(RAN_RED);
        gc.setLineWidth(Math.max(2.0d, Math.min(cellWidth, cellHeight) * 0.16d));
        gc.strokeRect(point.column() * cellWidth + inset, point.row() * cellHeight + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
    }

    private void drawRoles(MazeViewState state, double cellWidth, double cellHeight) {
        drawRole(state.entrance(), "S", RAN_BLUE, state, cellWidth, cellHeight);
        drawRole(state.exit(), "E", RAN_BLACK, state, cellWidth, cellHeight);
    }

    private void drawRole(GridPoint point, String label, Color stroke, MazeViewState state,
            double cellWidth, double cellHeight) {
        if (!inside(state, point)) return;
        double x = point.column() * cellWidth;
        double y = point.row() * cellHeight;
        double inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.14d);
        gc.setStroke(stroke);
        gc.setLineWidth(Math.max(1.5d, Math.min(cellWidth, cellHeight) * 0.13d));
        gc.strokeRect(x + inset, y + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        if (density != VisualDensity.DENSE && Math.min(cellWidth, cellHeight) >= 10.0d) {
            gc.setFill(RAN_BLACK);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD,
                    Math.max(8.0d, Math.min(cellWidth, cellHeight) * 0.46d)));
            gc.fillText(label, x + cellWidth / 2.0d,
                    y + cellHeight / 2.0d + Math.min(cellWidth, cellHeight) * 0.16d);
        }
    }

    private void drawSelection(MazeViewState state, double cellWidth, double cellHeight) {
        if (!inside(state, selectedCell)) return;
        double x = selectedCell.column() * cellWidth;
        double y = selectedCell.row() * cellHeight;
        double inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.06d);
        gc.setStroke(RAN_RED);
        gc.setLineWidth(Math.max(2.0d, Math.min(cellWidth, cellHeight) * 0.18d));
        gc.strokeRect(x + inset, y + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
    }

    public void setSelectionListener(Consumer<GridPoint> listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    public void clearSelection() {
        selectedCell = null;
        requestRender();
    }

    public GridPoint selectedCell() {
        return selectedCell;
    }

    public VisualDensity density() {
        return density;
    }

    @Override
    public void setViewportObstructionInsets(Insets insets) {
        surface.setObstructionInsets(insets);
    }

    @Override
    public void onVisualizationReset() {
        selectedCell = null;
        fillBackground();
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        canvas.setOnMouseClicked(null);
        surface.prefWidthProperty().unbind();
        surface.prefHeightProperty().unbind();
        super.dispose();
    }

    private VisualDensity densityFor(double cellSize) {
        if (cellSize < DENSE_CELL_THRESHOLD) return VisualDensity.DENSE;
        if (cellSize < COMPACT_CELL_THRESHOLD) return VisualDensity.COMPACT;
        return VisualDensity.DETAIL;
    }

    private static boolean inside(MazeViewState state, GridPoint point) {
        return point != null && point.row() >= 0 && point.row() < state.rows()
                && point.column() >= 0 && point.column() < state.columns();
    }
}
