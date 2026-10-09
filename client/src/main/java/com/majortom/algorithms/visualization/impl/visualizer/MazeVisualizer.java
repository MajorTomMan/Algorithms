package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.snapshot.MazeSnapshot;
import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.visualization.CanvasVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.MazeStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Project-owned Canvas/Grid maze renderer hosted by the shared GestureFX visualization surface. */
public final class MazeVisualizer extends CanvasVisualizer<MazeViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("MAZE");
    private static final StructureVisualization<MazeViewState> STRUCTURE_VISUALIZATION = new MazeStructureVisualization();
    private static final String GRID_ID = "maze:grid";
    private static final Color WALL_FILL = Color.web("#363B42");
    private static final Color ROAD_FILL = Color.web("#F8F8F6");
    private static final Color VISITED_FILL = Color.web("#C9DFF7");
    private static final Color VISITED_STROKE = RAN_BLUE;
    private static final Color GRID_STROKE = Color.web("#D9DDE2");
    private static final Color GRID_STROKE_COMPACT = GRID_STROKE;
    private static final Color PATH_EDGE = RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.46d);
    private static final Color GRAPH_EDGE_STROKE = RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.82d);
    private static final double DENSE_CELL_THRESHOLD = 8.0d;
    private static final double COMPACT_CELL_THRESHOLD = 14.0d;
    private static final Color ROLE_START_FILL = RAN_BLUE;
    private static final Color ROLE_EXIT_FILL = WALL_FILL;
    private static final Color MARKER_STROKE = RAN_BLACK;

    private final VisualizationSurface surface = new VisualizationSurface();
    private Consumer<GridPoint> selectionListener = ignored -> { };
    private GridPoint selectedCell;
    private MazeViewState renderedState;
    private VisualDensity density = VisualDensity.DETAIL;

    public MazeVisualizer() {
        installSurface(surface);
        surface.zoomProperty().addListener((observable, oldScale, newScale) -> {
            MazeViewState state = renderedState;
            if (state != null) {
                paint(state);
            }
        });
        canvas.widthProperty().unbind();
        canvas.heightProperty().unbind();
        surface.nodeLayer().getChildren().add(canvas);
        canvas.setOnMouseClicked(event -> {
            MazeViewState state = renderedState;
            if (state == null || state.rows() < 1 || state.columns() < 1) return;
            double cellWidth = canvas.getWidth() / state.columns();
            double cellHeight = canvas.getHeight() / state.rows();
            int column = (int) Math.floor(event.getX() / cellWidth);
            int row = (int) Math.floor(event.getY() / cellHeight);
            if (row < 0 || row >= state.rows() || column < 0 || column >= state.columns()) return;
            selectedCell = new GridPoint(row, column);
            selectionListener.accept(selectedCell);
            event.consume();
        });
        surface.markViewportPristine();
    }
    @Override
    public RenderSessionId sessionId() { return SESSION_ID; }


    @Override
    public CompletionStage<Void> commitLayout(MazeViewState state, LayoutPatch patch, RenderCommitContext context) {
        renderedState = state;
        ElementGeometry grid = patch.elements().get(GRID_ID);
        if (grid == null) {
            canvas.setWidth(1.0d);
            canvas.setHeight(1.0d);
        } else {
            canvas.setWidth(grid.width());
            canvas.setHeight(grid.height());
            canvas.relocate(grid.x(), grid.y());
        }
        paint(state);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> commitPresentation(MazeViewState state, RenderCommitContext context) {
        renderedState = state;
        paint(state);
        return CompletableFuture.completedFuture(null);
    }

    private void paint(MazeViewState state) {
        fillBackground();
        if (state.rows() < 1 || state.columns() < 1
                || state.openCells().size() < state.rows() * state.columns()) return;
        double cellWidth = canvas.getWidth() / state.columns();
        double cellHeight = canvas.getHeight() / state.rows();
        double screenCellSize = Math.min(cellWidth, cellHeight) * cameraScale();
        density = densityFor(screenCellSize);
        drawBaseGrid(state, cellWidth, cellHeight);
        drawVisited(state, cellWidth, cellHeight);
        if (state.graphBased()) drawGraphEdges(state, cellWidth, cellHeight);
        drawRoles(state, cellWidth, cellHeight);
        drawObserved(state, cellWidth, cellHeight);
        drawBacktracked(state, cellWidth, cellHeight);
        drawPath(state, cellWidth, cellHeight);
        drawCurrent(state, cellWidth, cellHeight);
        drawRoleLabels(state, cellWidth, cellHeight);
        drawSelection(state, cellWidth, cellHeight);
    }

    private void fillBackground() {
        gc.setEffect(null);
        gc.setFill(ROAD_FILL);
        gc.fillRect(0.0d, 0.0d, canvas.getWidth(), canvas.getHeight());
    }

    private void drawBaseGrid(MazeViewState state, double cellWidth, double cellHeight) {
        if (density == VisualDensity.DENSE) {
            drawDenseBase(state, cellWidth, cellHeight);
            return;
        }

        gc.setLineWidth(1.0d);
        if (density == VisualDensity.DETAIL) {
            gc.setStroke(GRID_STROKE);
        } else {
            gc.setStroke(GRID_STROKE_COMPACT);
        }
        for (int row = 0; row < state.rows(); row++) {
            for (int column = 0; column < state.columns(); column++) {
                int index = row * state.columns() + column;
                double x = column * cellWidth;
                double y = row * cellHeight;
                if (state.openCells().get(index)) {
                    gc.setFill(ROAD_FILL);
                } else {
                    gc.setFill(WALL_FILL);
                }
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
        gc.setFill(ROAD_FILL);
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
        gc.setStroke(GRAPH_EDGE_STROKE);
        double cellSize = Math.min(cellWidth, cellHeight);
        gc.setLineWidth(graphEdgeStrokeWidth(cellSize));
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
        gc.setFill(VISITED_FILL);
        double cellSize = Math.min(cellWidth, cellHeight);
        double inset;
        if (density == VisualDensity.DENSE) {
            inset = 0.0d;
        } else {
            inset = Math.max(1.0d, Math.min(cellWidth, cellHeight) * 0.12d);
        }
        for (GridPoint point : state.visited()) {
            if (!inside(state, point)) continue;
            double x = point.column() * cellWidth;
            double y = point.row() * cellHeight;
            gc.fillRect(x + inset, y + inset,
                    Math.max(0.0d, cellWidth - inset * 2.0d),
                    Math.max(0.0d, cellHeight - inset * 2.0d));
            if (density != VisualDensity.DENSE) {
                gc.setStroke(VISITED_STROKE);
                gc.setLineWidth(visitedStrokeWidth(cellSize));
                gc.strokeRect(x + inset, y + inset,
                        Math.max(0.0d, cellWidth - inset * 2.0d),
                        Math.max(0.0d, cellHeight - inset * 2.0d));
            }
        }
        if (density == VisualDensity.DENSE) {
            double markerSize = Math.min(cellSize * 0.34d, worldLengthForScreenPixels(0.9d));
            gc.setFill(VISITED_STROKE);
            for (GridPoint point : state.visited()) {
                if (!inside(state, point)) continue;
                double centerX = (point.column() + 0.5d) * cellWidth;
                double centerY = (point.row() + 0.5d) * cellHeight;
                gc.fillRect(centerX - markerSize / 2.0d, centerY - markerSize / 2.0d,
                        markerSize, markerSize);
            }
        }
    }

    private void drawObserved(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.observed();
        drawMarker(state, point, cellWidth, cellHeight, false);
    }

    private void drawBacktracked(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.backtracked();
        drawMarker(state, point, cellWidth, cellHeight, true);
    }

    private void drawMarker(MazeViewState state, GridPoint point, double cellWidth,
            double cellHeight, boolean diamond) {
        if (!inside(state, point)) return;
        double cellSize = Math.min(cellWidth, cellHeight);
        double markerSize = density == VisualDensity.DENSE
                ? Math.min(cellSize * 0.68d, worldLengthForScreenPixels(2.0d))
                : Math.max(1.0d, Math.min(14.0d, cellSize * 0.68d));
        double centerX = (point.column() + 0.5d) * cellWidth;
        double centerY = (point.row() + 0.5d) * cellHeight;
        double half = markerSize / 2.0d;

        gc.save();
        gc.setFill(RAN_YELLOW);
        gc.setStroke(MARKER_STROKE);
        gc.setLineWidth(density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.0d)
                : Math.max(1.0d, Math.min(2.0d, cellSize * 0.08d)));
        if (diamond) {
            double[] xPoints = {centerX, centerX + half, centerX, centerX - half};
            double[] yPoints = {centerY - half, centerY, centerY + half, centerY};
            gc.fillPolygon(xPoints, yPoints, 4);
            if (markerSize >= 3.0d) gc.strokePolygon(xPoints, yPoints, 4);
        } else {
            gc.fillOval(centerX - half, centerY - half, markerSize, markerSize);
            if (markerSize >= 3.0d) {
                gc.strokeOval(centerX - half, centerY - half, markerSize, markerSize);
            }
        }
        gc.restore();
    }

    private void drawPath(MazeViewState state, double cellWidth, double cellHeight) {
        if (state.path().isEmpty()) return;
        gc.save();
        double cellSize = Math.min(cellWidth, cellHeight);
        double lineWidth = density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.5d)
                : Math.max(2.0d, cellSize * 0.24d);
        double edgeWidth = density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 2.8d)
                : Math.max(lineWidth + 1.5d, cellSize * 0.30d);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        gc.setStroke(PATH_EDGE);
        gc.setFill(PATH_EDGE);
        gc.setLineWidth(edgeWidth);
        drawPathGeometry(state, cellWidth, cellHeight, edgeWidth);
        gc.setStroke(RAN_YELLOW);
        gc.setFill(RAN_YELLOW);
        gc.setLineWidth(lineWidth);
        drawPathGeometry(state, cellWidth, cellHeight, lineWidth);
        gc.restore();
    }

    private void drawPathGeometry(MazeViewState state, double cellWidth, double cellHeight,
            double lineWidth) {
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
    }

    private void drawCurrent(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.active();
        if (!inside(state, point)) return;
        double cellSize = Math.min(cellWidth, cellHeight);
        double inset = density == VisualDensity.DENSE
                ? Math.min(cellSize * 0.36d, worldLengthForScreenPixels(1.2d))
                : Math.max(1.0d, cellSize * 0.10d);
        gc.save();
        gc.setStroke(RAN_RED);
        gc.setLineWidth(density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.4d)
                : Math.max(2.0d, cellSize * 0.16d));
        gc.strokeRect(point.column() * cellWidth + inset, point.row() * cellHeight + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        gc.restore();
    }

    private void drawRoles(MazeViewState state, double cellWidth, double cellHeight) {
        drawRoleBadge(state.entrance(), ROLE_START_FILL, state, cellWidth, cellHeight);
        drawRoleBadge(state.exit(), ROLE_EXIT_FILL, state, cellWidth, cellHeight);
    }

    private void drawRoleBadge(GridPoint point, Color fill, MazeViewState state,
            double cellWidth, double cellHeight) {
        if (!inside(state, point)) return;
        double x = point.column() * cellWidth;
        double y = point.row() * cellHeight;
        double cellSize = Math.min(cellWidth, cellHeight);
        double inset = density == VisualDensity.DENSE
                ? Math.min(cellSize * 0.16d, worldLengthForScreenPixels(0.35d))
                : Math.max(1.0d, cellSize * 0.13d);
        double width = Math.max(0.0d, cellWidth - inset * 2.0d);
        double height = Math.max(0.0d, cellHeight - inset * 2.0d);

        gc.save();
        gc.setFill(fill);
        double radius = Math.min(8.0d, cellSize * 0.30d);
        gc.fillRoundRect(x + inset, y + inset, width, height, radius, radius);
        if (density == VisualDensity.DENSE) {
            gc.setStroke(RAN_WHITE);
            gc.setLineWidth(denseStrokeWidth(cellSize, 1.0d));
            gc.strokeRoundRect(x + inset, y + inset, width, height, radius, radius);
        }
        gc.restore();
    }

    private void drawRoleLabels(MazeViewState state, double cellWidth, double cellHeight) {
        drawRoleLabel(state.entrance(), "S", state, cellWidth, cellHeight);
        drawRoleLabel(state.exit(), "E", state, cellWidth, cellHeight);
    }

    private void drawRoleLabel(GridPoint point, String label, MazeViewState state,
            double cellWidth, double cellHeight) {
        if (density == VisualDensity.DENSE || !inside(state, point)) return;
        double cellSize = Math.min(cellWidth, cellHeight);
        if (cellSize < 10.0d) return;
        double x = point.column() * cellWidth;
        double y = point.row() * cellHeight;
        gc.save();
        gc.setFill(RAN_WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD,
                Math.min(14.0d, Math.max(7.0d, cellSize * 0.46d))));
        gc.fillText(label, x + cellWidth / 2.0d, y + cellHeight / 2.0d);
        gc.restore();
    }

    private void drawSelection(MazeViewState state, double cellWidth, double cellHeight) {
        if (!inside(state, selectedCell)) return;
        double x = selectedCell.column() * cellWidth;
        double y = selectedCell.row() * cellHeight;
        double cellSize = Math.min(cellWidth, cellHeight);
        boolean sameAsCurrent = selectedCell.equals(state.active());
        gc.save();
        gc.setStroke(RAN_RED);
        gc.setLineWidth(density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.0d)
                : Math.max(2.0d, cellSize * 0.14d));
        double inset = density == VisualDensity.DENSE
                ? Math.min(cellSize * 0.12d, worldLengthForScreenPixels(0.45d))
                : sameAsCurrent
                        ? Math.max(3.0d, cellSize * 0.24d)
                        : Math.max(1.0d, cellSize * 0.06d);
        double dash = density == VisualDensity.DENSE
                ? worldLengthForScreenPixels(1.6d)
                : Math.max(3.0d, cellSize * 0.24d);
        gc.setLineDashes(dash, dash * 0.70d);
        gc.strokeRect(x + inset, y + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        gc.restore();
    }

    public void setSelectionListener(Consumer<GridPoint> listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    public void clearSelection() {
        selectedCell = null;
    }

    public boolean showSelection(GridPoint point) {
        if (point == null || (renderedState != null && !inside(renderedState, point))) {
            return false;
        }
        selectedCell = point;
        return true;
    }

    public GridPoint selectedCell() {
        return selectedCell;
    }

    public VisualDensity density() {
        return density;
    }

    @Override
    public StructureVisualization<MazeViewState> structureVisualization() {
        return STRUCTURE_VISUALIZATION;
    }

    @Override
    public FxSurfaceAdapter fxSurfaceAdapter() {
        return surface;
    }
    @Override
    public void onVisualizationReset() {
        super.onVisualizationReset();
        selectedCell = null;
        renderedState = null;
        fillBackground();
        surface.reset();
        surface.markViewportPristine();
    }

    private VisualDensity densityFor(double cellSize) {
        if (cellSize < DENSE_CELL_THRESHOLD) return VisualDensity.DENSE;
        if (cellSize < COMPACT_CELL_THRESHOLD) return VisualDensity.COMPACT;
        return VisualDensity.DETAIL;
    }

    private double cameraScale() {
        return Math.max(0.01d, surface.cameraState().scale());
    }

    private double worldLengthForScreenPixels(double pixels) {
        return pixels / cameraScale();
    }

    private double denseStrokeWidth(double cellSize, double screenPixels) {
        return Math.min(cellSize * 0.50d, worldLengthForScreenPixels(screenPixels));
    }

    private double graphEdgeStrokeWidth(double cellSize) {
        if (density == VisualDensity.DENSE) {
            return denseStrokeWidth(cellSize, 1.25d);
        }
        double screenCellSize = cellSize * cameraScale();
        double maxScreenWidth = screenCellSize * 0.16d;
        double targetScreenWidth = Math.min(1.75d,
                Math.max(1.25d, screenCellSize * 0.12d));
        return worldLengthForScreenPixels(Math.min(maxScreenWidth, targetScreenWidth));
    }

    private double visitedStrokeWidth(double cellSize) {
        double screenCellSize = cellSize * cameraScale();
        double maxScreenWidth = screenCellSize * 0.14d;
        double targetScreenWidth = Math.min(1.15d,
                Math.max(0.85d, screenCellSize * 0.06d));
        return worldLengthForScreenPixels(Math.min(maxScreenWidth, targetScreenWidth));
    }

    private static boolean inside(MazeViewState state, GridPoint point) {
        return point != null && point.row() >= 0 && point.row() < state.rows()
                && point.column() >= 0 && point.column() < state.columns();
    }
}
