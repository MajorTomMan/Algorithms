package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.structure.maze.GridPoint;
import com.majortom.algorithms.visualization.CanvasVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.semantic.MazeStructureVisualization;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualizationSurface;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.ElementGeometry;
import com.majortom.algorithms.visualization.render.api.LayoutResult;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Project-owned Canvas/Grid maze renderer hosted by the shared GestureFX visualization surface. */
public final class MazeVisualizer extends CanvasVisualizer<MazeViewState> {
    private static final RenderSessionId SESSION_ID = RenderSessionId.of("MAZE");
    private static final StructureVisualization<MazeViewState> STRUCTURE_VISUALIZATION = new MazeStructureVisualization();
    private static final String GRID_ID = "maze:grid";
    private static final int TILE_CELLS = 16;
    private static final int[][] PATH_NEIGHBORS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final Color WALL_FILL = Color.web("#363B42");
    private static final Color ROAD_FILL = Color.web("#F8F8F6");
    private static final Color VISITED_FILL = Color.web("#C9DFF7");
    private static final Color VISITED_STROKE = RAN_BLUE;
    private static final Color GRID_STROKE = Color.web("#D9DDE2");
    private static final Color GRID_STROKE_COMPACT = GRID_STROKE;
    private static final Color PATH_EDGE = RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.46d);
    private static final double DENSE_CELL_THRESHOLD = 8.0d;
    private static final double COMPACT_CELL_THRESHOLD = 14.0d;
    private static final Color ROLE_START_FILL = RAN_BLUE;
    private static final Color ROLE_EXIT_FILL = WALL_FILL;
    private static final Color MARKER_STROKE = RAN_BLACK;

    private final VisualizationSurface surface = new VisualizationSurface();
    private final Group tileLayer = new Group();
    private final List<MazeTile> tiles = new ArrayList<>();
    private GraphicsContext painter;
    private int tileColumns;
    private int tileRows;
    private int terrainRows = -1;
    private int terrainColumns = -1;
    private double worldWidth;
    private double worldHeight;
    private double tiledCellWidth = Double.NaN;
    private double tiledCellHeight = Double.NaN;
    private final Path gridLines = new Path();
    private int gridRows = -1;
    private int gridColumns = -1;
    private double gridWidth = Double.NaN;
    private double gridHeight = Double.NaN;
    private Consumer<GridPoint> selectionListener = ignored -> { };
    private GridPoint selectedCell;
    private MazeViewState renderedState;
    private MazeViewState paintedState;
    private GridPoint paintedSelection;
    private double paintedWidth = Double.NaN;
    private double paintedHeight = Double.NaN;
    private VisualDensity density = VisualDensity.DETAIL;

    public MazeVisualizer() {
        installSurface(surface);
        canvas.widthProperty().unbind();
        canvas.heightProperty().unbind();
        // Keeping the inherited Canvas detached avoids allocating a single huge texture.
        surface.nodeLayer().getChildren().add(tileLayer);

        // A single vector path replaces one border stroke for every maze cell.
        // It moves with the same GestureFX world transform as the terrain Canvas.
        gridLines.setFill(null);
        gridLines.setStroke(GRID_STROKE);
        gridLines.setStrokeWidth(1.0d);
        gridLines.setMouseTransparent(true);
        gridLines.setManaged(false);
        gridLines.setVisible(false);
        surface.decorationLayer().getChildren().add(gridLines);

        surface.zoomProperty().addListener((observable, oldScale, newScale) -> {
            MazeViewState state = renderedState;
            if (state == null || state.rows() < 1 || state.columns() < 1)
                return;
            // The scene graph already scales both terrain and grid. Only a density
            // transition needs a new terrain image, not every scroll tick.
            double screenCellSize = Math.min(worldWidth / state.columns(),
                    worldHeight / state.rows()) * cameraScale();
            gridLines.setStrokeWidth(Math.min(1.0d, 1.0d / cameraScale()));
            if (densityFor(screenCellSize) != density)
                paint(state);
        });
        tileLayer.setOnMouseClicked(event -> {
            MazeViewState state = renderedState;
            if (state == null || state.rows() < 1 || state.columns() < 1) return;
            double cellWidth = worldWidth / state.columns();
            double cellHeight = worldHeight / state.rows();
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
    public CompletionStage<Void> commitLayout(MazeViewState state, LayoutResult patch, RenderCommitContext context) {
        renderedState = state;
        paintedState = null;
        ElementGeometry grid = patch.elements().get(GRID_ID);
        if (grid == null) {
            worldWidth = 1.0d;
            worldHeight = 1.0d;
            tileLayer.setTranslateX(0.0d);
            tileLayer.setTranslateY(0.0d);
            gridLines.setTranslateX(0.0d);
            gridLines.setTranslateY(0.0d);
        } else {
            worldWidth = grid.width();
            worldHeight = grid.height();
            tileLayer.setTranslateX(grid.x());
            tileLayer.setTranslateY(grid.y());
            gridLines.setTranslateX(grid.x());
            gridLines.setTranslateY(grid.y());
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
        if (state.rows() < 1 || state.columns() < 1
                || state.openCells().size() < state.rows() * state.columns()) {
            paintedState = null;
            tileLayer.getChildren().clear();
            tiles.clear();
            gridLines.setVisible(false);
            return;
        }
        double width = worldWidth;
        double height = worldHeight;
        double cellWidth = width / state.columns();
        double cellHeight = height / state.rows();
        VisualDensity targetDensity = densityFor(
                Math.min(cellWidth, cellHeight) * cameraScale());
        boolean rebuiltTiles = ensureTiles(state, cellWidth, cellHeight);
        MazeViewState previous = paintedState;
        boolean sameGeometry = !rebuiltTiles && previous != null
                && previous.rows() == state.rows() && previous.columns() == state.columns()
                && width == paintedWidth && height == paintedHeight
                && targetDensity == density;

        density = targetDensity;
        refreshGridLines(state, width, height, cellWidth, cellHeight);
        gridLines.setVisible(density != VisualDensity.DENSE);
        if (sameGeometry) {
            BitSet dirty = new BitSet(state.rows() * state.columns());
            state.forEachChangedCell(previous, dirty::set);
            markDirty(dirty, state, paintedSelection);
            markDirty(dirty, state, selectedCell);
            // Seeking a long way backward can dirty most of the map.
            if (dirty.cardinality() <= Math.max(128, state.rows() * state.columns() / 4)) {
                // Many dirty cells in the same tile are cheaper to redraw together
                // than to issue dozens of save/clip/restore commands.
                int[] changesPerTile = new int[tiles.size()];
                for (int index = dirty.nextSetBit(0); index >= 0;
                        index = dirty.nextSetBit(index + 1)) {
                    int row = index / state.columns();
                    int column = index % state.columns();
                    changesPerTile[tileIndex(row, column)]++;
                }
                boolean[] repaintedTiles = new boolean[tiles.size()];
                for (int index = 0; index < tiles.size(); index++) {
                    MazeTile tile = tiles.get(index);
                    int cellCount = (tile.lastRow - tile.firstRow)
                            * (tile.lastColumn - tile.firstColumn);
                    int threshold = Math.max(8, Math.min(48, cellCount / 4));
                    if (changesPerTile[index] >= threshold) {
                        paintWholeTile(state, tile, cellWidth, cellHeight);
                        repaintedTiles[index] = true;
                    }
                }
                for (int index = dirty.nextSetBit(0); index >= 0;
                        index = dirty.nextSetBit(index + 1)) {
                    int row = index / state.columns();
                    int column = index % state.columns();
                    if (!repaintedTiles[tileIndex(row, column)])
                        paintCell(state, row, column, cellWidth, cellHeight);
                }
                rememberPaint(state, width, height);
                return;
            }
        }

        for (MazeTile tile : tiles)
            paintWholeTile(state, tile, cellWidth, cellHeight);
        rememberPaint(state, width, height);
    }

    /**
     * The 99x99 maze uses small 16x16-cell Canvas tiles. When a few cells
     * change, only their owning textures become dirty; GestureFX can cull
     * off-screen tiles while the user is zoomed in.
     */
    private boolean ensureTiles(MazeViewState state, double cellWidth, double cellHeight) {
        if (terrainRows == state.rows() && terrainColumns == state.columns()
                && tiledCellWidth == cellWidth && tiledCellHeight == cellHeight
                && !tiles.isEmpty())
            return false;
        tiles.clear();
        tileLayer.getChildren().clear();
        tileColumns = (state.columns() + TILE_CELLS - 1) / TILE_CELLS;
        tileRows = (state.rows() + TILE_CELLS - 1) / TILE_CELLS;
        for (int tileRow = 0; tileRow < tileRows; tileRow++) {
            for (int tileColumn = 0; tileColumn < tileColumns; tileColumn++) {
                int firstRow = tileRow * TILE_CELLS;
                int firstColumn = tileColumn * TILE_CELLS;
                int lastRow = Math.min(state.rows(), firstRow + TILE_CELLS);
                int lastColumn = Math.min(state.columns(), firstColumn + TILE_CELLS);
                double x = firstColumn * cellWidth;
                double y = firstRow * cellHeight;
                Canvas tileCanvas = new Canvas((lastColumn - firstColumn) * cellWidth,
                        (lastRow - firstRow) * cellHeight);
                tileCanvas.setLayoutX(x);
                tileCanvas.setLayoutY(y);
                GraphicsContext tileGraphics = tileCanvas.getGraphicsContext2D();
                tileGraphics.setTransform(1.0d, 0.0d, 0.0d, 1.0d, -x, -y);
                tiles.add(new MazeTile(tileCanvas, tileGraphics, firstRow, lastRow,
                        firstColumn, lastColumn, x, y));
                tileLayer.getChildren().add(tileCanvas);
            }
        }
        terrainRows = state.rows();
        terrainColumns = state.columns();
        tiledCellWidth = cellWidth;
        tiledCellHeight = cellHeight;
        paintedState = null;
        return true;
    }

    /** Renders one tile in layered order, preserving the existing maze styling. */
    private void paintWholeTile(MazeViewState state, MazeTile tile,
            double cellWidth, double cellHeight) {
        painter = tile.graphics;
        painter.save();
        painter.setEffect(null);
        painter.setFill(WALL_FILL);
        painter.fillRect(tile.x, tile.y, tile.canvas.getWidth(), tile.canvas.getHeight());

        // Static terrain first, then all state highlights.
        for (int row = tile.firstRow; row < tile.lastRow; row++) {
            for (int column = tile.firstColumn; column < tile.lastColumn; column++) {
                int index = row * state.columns() + column;
                boolean open = state.openCells().get(index);
                if (density == VisualDensity.DENSE) {
                    if (open) {
                        painter.setFill(ROAD_FILL);
                        double x0 = Math.floor(column * cellWidth);
                        double y0 = Math.floor(row * cellHeight);
                        double x1 = Math.ceil((column + 1) * cellWidth);
                        double y1 = Math.ceil((row + 1) * cellHeight);
                        painter.fillRect(x0, y0, Math.max(1.0d, x1 - x0),
                                Math.max(1.0d, y1 - y0));
                    }
                } else if (open) {
                    painter.setFill(ROAD_FILL);
                    painter.fillRect(column * cellWidth, row * cellHeight,
                            cellWidth, cellHeight);
                }
            }
        }

        for (int row = tile.firstRow; row < tile.lastRow; row++) {
            for (int column = tile.firstColumn; column < tile.lastColumn; column++) {
                GridPoint point = new GridPoint(row, column);
                if (state.visited().contains(point))
                    drawVisitedCell(point, cellWidth, cellHeight);
            }
        }
        if (tile.contains(state.entrance()))
            drawRoleBadge(state.entrance(), ROLE_START_FILL, state, cellWidth, cellHeight);
        if (tile.contains(state.exit()))
            drawRoleBadge(state.exit(), ROLE_EXIT_FILL, state, cellWidth, cellHeight);
        if (tile.contains(state.observed()))
            drawMarker(state, state.observed(), cellWidth, cellHeight, false);
        if (tile.contains(state.backtracked()))
            drawMarker(state, state.backtracked(), cellWidth, cellHeight, true);
        for (GridPoint point : state.path()) {
            if (tile.contains(point))
                drawPathCell(state, point, cellWidth, cellHeight);
        }
        if (tile.contains(state.active()))
            drawCurrent(state, cellWidth, cellHeight);
        if (tile.contains(state.entrance()))
            drawRoleLabel(state.entrance(), "S", state, cellWidth, cellHeight);
        if (tile.contains(state.exit()))
            drawRoleLabel(state.exit(), "E", state, cellWidth, cellHeight);
        if (tile.contains(selectedCell))
            drawSelection(state, cellWidth, cellHeight);
        painter.restore();
    }

    private int tileIndex(int row, int column) {
        return (row / TILE_CELLS) * tileColumns + column / TILE_CELLS;
    }

    private MazeTile tileFor(int row, int column) {
        return tiles.get(tileIndex(row, column));
    }

    private static final class MazeTile {
        final Canvas canvas;
        final GraphicsContext graphics;
        final int firstRow;
        final int lastRow;
        final int firstColumn;
        final int lastColumn;
        final double x;
        final double y;

        MazeTile(Canvas canvas, GraphicsContext graphics, int firstRow, int lastRow,
                int firstColumn, int lastColumn, double x, double y) {
            this.canvas = canvas;
            this.graphics = graphics;
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.firstColumn = firstColumn;
            this.lastColumn = lastColumn;
            this.x = x;
            this.y = y;
        }

        boolean contains(GridPoint point) {
            return point != null && point.row() >= firstRow && point.row() < lastRow
                    && point.column() >= firstColumn && point.column() < lastColumn;
        }
    }

    private void rememberPaint(MazeViewState state, double width, double height) {
        paintedState = state;
        paintedSelection = selectedCell;
        paintedWidth = width;
        paintedHeight = height;
    }

    /** Rebuilds the line geometry only when world dimensions change, never per event. */
    private void refreshGridLines(MazeViewState state, double width, double height,
            double cellWidth, double cellHeight) {
        if (gridRows == state.rows() && gridColumns == state.columns()
                && gridWidth == width && gridHeight == height)
            return;
        List<PathElement> segments = new ArrayList<>(2 * (state.rows() + state.columns() + 2));
        for (int row = 0; row <= state.rows(); row++) {
            double y = row * cellHeight;
            segments.add(new MoveTo(0.0d, y));
            segments.add(new LineTo(width, y));
        }
        for (int column = 0; column <= state.columns(); column++) {
            double x = column * cellWidth;
            segments.add(new MoveTo(x, 0.0d));
            segments.add(new LineTo(x, height));
        }
        gridLines.getElements().setAll(segments);
        gridRows = state.rows();
        gridColumns = state.columns();
        gridWidth = width;
        gridHeight = height;
    }

    private static void markDirty(BitSet dirty, MazeViewState state, GridPoint point) {
        if (inside(state, point))
            dirty.set(point.row() * state.columns() + point.column());
    }

    /** Repaints only the affected tile. Clip prevents neighboring overlays being erased. */
    private void paintCell(MazeViewState state, int row, int column,
            double cellWidth, double cellHeight) {
        painter = tileFor(row, column).graphics;
        double x = column * cellWidth;
        double y = row * cellHeight;
        GridPoint point = new GridPoint(row, column);
        painter.save();
        painter.beginPath();
        painter.rect(x, y, cellWidth, cellHeight);
        painter.clip();

        if (density == VisualDensity.DENSE) {
            painter.setFill(state.openCells().get(row * state.columns() + column)
                    ? ROAD_FILL : WALL_FILL);
            double x0 = Math.floor(x), y0 = Math.floor(y);
            double x1 = Math.ceil(x + cellWidth), y1 = Math.ceil(y + cellHeight);
            painter.fillRect(x0, y0, Math.max(1.0d, x1 - x0), Math.max(1.0d, y1 - y0));
        } else {
            painter.setFill(state.openCells().get(row * state.columns() + column)
                    ? ROAD_FILL : WALL_FILL);
            painter.fillRect(x, y, cellWidth, cellHeight);
        }

        if (state.visited().contains(point))
            drawVisitedCell(point, cellWidth, cellHeight);

        if (point.equals(state.entrance()))
            drawRoleBadge(point, ROLE_START_FILL, state, cellWidth, cellHeight);
        if (point.equals(state.exit()))
            drawRoleBadge(point, ROLE_EXIT_FILL, state, cellWidth, cellHeight);

        if (point.equals(state.observed()))
            drawMarker(state, point, cellWidth, cellHeight, false);
        if (point.equals(state.backtracked()))
            drawMarker(state, point, cellWidth, cellHeight, true);
        if (state.path().contains(point))
            drawPathCell(state, point, cellWidth, cellHeight);
        if (point.equals(state.active()))
            drawCurrent(state, cellWidth, cellHeight);
        if (point.equals(state.entrance()))
            drawRoleLabel(point, "S", state, cellWidth, cellHeight);
        if (point.equals(state.exit()))
            drawRoleLabel(point, "E", state, cellWidth, cellHeight);
        if (point.equals(selectedCell))
            drawSelection(state, cellWidth, cellHeight);
        painter.restore();
    }

    private void drawVisitedCell(GridPoint point, double cellWidth, double cellHeight) {
        double cellSize = Math.min(cellWidth, cellHeight);
        double inset = density == VisualDensity.DENSE ? 0.0d
                : Math.max(1.0d, cellSize * 0.12d);
        double x = point.column() * cellWidth;
        double y = point.row() * cellHeight;
        painter.setFill(VISITED_FILL);
        painter.fillRect(x + inset, y + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        painter.setFill(VISITED_STROKE);
        if (density == VisualDensity.DENSE) {
            double markerSize = Math.min(cellSize * 0.34d, worldLengthForScreenPixels(0.9d));
            double centerX = (point.column() + 0.5d) * cellWidth;
            double centerY = (point.row() + 0.5d) * cellHeight;
            painter.fillRect(centerX - markerSize / 2.0d, centerY - markerSize / 2.0d,
                    markerSize, markerSize);
        } else {
            painter.setStroke(VISITED_STROKE);
            painter.setLineWidth(visitedStrokeWidth(cellSize));
            painter.strokeRect(x + inset, y + inset,
                    Math.max(0.0d, cellWidth - inset * 2.0d),
                    Math.max(0.0d, cellHeight - inset * 2.0d));
        }
    }

    /** Each tile draws its own half of path segments, clipped at cell borders. */
    private void drawPathCell(MazeViewState state, GridPoint point,
            double cellWidth, double cellHeight) {
        double cellSize = Math.min(cellWidth, cellHeight);
        double lineWidth = density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.5d) : Math.max(2.0d, cellSize * 0.24d);
        double edgeWidth = density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 2.8d) : Math.max(lineWidth + 1.5d, cellSize * 0.30d);
        painter.save();
        painter.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        painter.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        painter.setStroke(PATH_EDGE);
        painter.setFill(PATH_EDGE);
        painter.setLineWidth(edgeWidth);
        drawPathTileGeometry(state, point, cellWidth, cellHeight, edgeWidth);
        painter.setStroke(RAN_YELLOW);
        painter.setFill(RAN_YELLOW);
        painter.setLineWidth(lineWidth);
        drawPathTileGeometry(state, point, cellWidth, cellHeight, lineWidth);
        painter.restore();
    }

    private void drawPathTileGeometry(MazeViewState state, GridPoint point,
            double cellWidth, double cellHeight, double lineWidth) {
        double x = (point.column() + 0.5d) * cellWidth;
        double y = (point.row() + 0.5d) * cellHeight;
        for (int[] offset : PATH_NEIGHBORS) {
            int row = point.row() + offset[0];
            int column = point.column() + offset[1];
            if (row >= 0 && column >= 0 && row < state.rows() && column < state.columns()
                    && state.path().contains(new GridPoint(row, column))) {
                painter.strokeLine(x, y, x + offset[1] * cellWidth, y + offset[0] * cellHeight);
            }
        }
        painter.fillOval(x - lineWidth / 2.0d, y - lineWidth / 2.0d, lineWidth, lineWidth);
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

        painter.save();
        painter.setFill(RAN_YELLOW);
        painter.setStroke(MARKER_STROKE);
        painter.setLineWidth(density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.0d)
                : Math.max(1.0d, Math.min(2.0d, cellSize * 0.08d)));
        if (diamond) {
            double[] xPoints = {centerX, centerX + half, centerX, centerX - half};
            double[] yPoints = {centerY - half, centerY, centerY + half, centerY};
            painter.fillPolygon(xPoints, yPoints, 4);
            if (markerSize >= 3.0d) painter.strokePolygon(xPoints, yPoints, 4);
        } else {
            painter.fillOval(centerX - half, centerY - half, markerSize, markerSize);
            if (markerSize >= 3.0d) {
                painter.strokeOval(centerX - half, centerY - half, markerSize, markerSize);
            }
        }
        painter.restore();
    }

    private void drawCurrent(MazeViewState state, double cellWidth, double cellHeight) {
        GridPoint point = state.active();
        if (!inside(state, point)) return;
        double cellSize = Math.min(cellWidth, cellHeight);
        double inset = density == VisualDensity.DENSE
                ? Math.min(cellSize * 0.36d, worldLengthForScreenPixels(1.2d))
                : Math.max(1.0d, cellSize * 0.10d);
        painter.save();
        painter.setStroke(RAN_RED);
        painter.setLineWidth(density == VisualDensity.DENSE
                ? denseStrokeWidth(cellSize, 1.4d)
                : Math.max(2.0d, cellSize * 0.16d));
        painter.strokeRect(point.column() * cellWidth + inset, point.row() * cellHeight + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        painter.restore();
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

        painter.save();
        painter.setFill(fill);
        double radius = Math.min(8.0d, cellSize * 0.30d);
        painter.fillRoundRect(x + inset, y + inset, width, height, radius, radius);
        if (density == VisualDensity.DENSE) {
            painter.setStroke(RAN_WHITE);
            painter.setLineWidth(denseStrokeWidth(cellSize, 1.0d));
            painter.strokeRoundRect(x + inset, y + inset, width, height, radius, radius);
        }
        painter.restore();
    }

    private void drawRoleLabel(GridPoint point, String label, MazeViewState state,
            double cellWidth, double cellHeight) {
        if (density == VisualDensity.DENSE || !inside(state, point)) return;
        double cellSize = Math.min(cellWidth, cellHeight);
        if (cellSize < 10.0d) return;
        double x = point.column() * cellWidth;
        double y = point.row() * cellHeight;
        painter.save();
        painter.setFill(RAN_WHITE);
        painter.setTextAlign(TextAlignment.CENTER);
        painter.setTextBaseline(VPos.CENTER);
        painter.setFont(Font.font("Consolas", FontWeight.BOLD,
                Math.min(14.0d, Math.max(7.0d, cellSize * 0.46d))));
        painter.fillText(label, x + cellWidth / 2.0d, y + cellHeight / 2.0d);
        painter.restore();
    }

    private void drawSelection(MazeViewState state, double cellWidth, double cellHeight) {
        if (!inside(state, selectedCell)) return;
        double x = selectedCell.column() * cellWidth;
        double y = selectedCell.row() * cellHeight;
        double cellSize = Math.min(cellWidth, cellHeight);
        boolean sameAsCurrent = selectedCell.equals(state.active());
        painter.save();
        painter.setStroke(RAN_RED);
        painter.setLineWidth(density == VisualDensity.DENSE
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
        painter.setLineDashes(dash, dash * 0.70d);
        painter.strokeRect(x + inset, y + inset,
                Math.max(0.0d, cellWidth - inset * 2.0d),
                Math.max(0.0d, cellHeight - inset * 2.0d));
        painter.restore();
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
        // No giant detached Canvas is cleared on reset.
        selectedCell = null;
        renderedState = null;
        paintedState = null;
        paintedSelection = null;
        tiles.clear();
        tileLayer.getChildren().clear();
        terrainRows = -1;
        terrainColumns = -1;
        tiledCellWidth = Double.NaN;
        tiledCellHeight = Double.NaN;
        gridRows = -1;
        gridColumns = -1;
        gridLines.getElements().clear();
        gridLines.setVisible(false);
        surface.reset();
        surface.markViewportPristine();
    }

    @Override
    public void dispose() {
        if (isDisposed()) return;
        tileLayer.setOnMouseClicked(null);
        tileLayer.getChildren().clear();
        tiles.clear();
        gridLines.getElements().clear();
        super.dispose();
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
