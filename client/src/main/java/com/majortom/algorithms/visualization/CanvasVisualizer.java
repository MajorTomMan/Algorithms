package com.majortom.algorithms.visualization;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;

/** Canvas-specific rendering support. SceneGraph visualizers should extend BaseVisualizer directly. */
public abstract class CanvasVisualizer<S> extends BaseVisualizer<S> {
    public static final Color RAN_BLACK = Color.web("#08090A");
    public static final Color RAN_WHITE = Color.WHITE;
    public static final Color RAN_RED = Color.web("#F51B23");
    public static final Color RAN_BLUE = Color.web("#1769D3");
    public static final Color RAN_YELLOW = Color.web("#F5B400");
    public static final Color RAN_GRAY = Color.web("#444444");

    protected final Canvas canvas = new Canvas();
    protected final GraphicsContext gc = canvas.getGraphicsContext2D();
    protected final Glow highIntensityGlow = new Glow(0.8);

    protected CanvasVisualizer() {
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
    }

    public void clear() {
        gc.setEffect(null);
        gc.setFill(RAN_BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    protected Color getContrastStrokeColor(Color background) {
        if (background.equals(RAN_WHITE)) return RAN_BLACK;
        if (background.equals(RAN_BLUE)) return RAN_WHITE.deriveColor(0, 0.5, 1, 0.8);
        return Color.rgb(10, 0, 0, 0.85);
    }

    protected void applyFocusEffect() {
        gc.save();
        gc.setEffect(highIntensityGlow);
    }

    protected void releaseEffect() { gc.restore(); }

    protected void drawClanMon(double mx, double my, double size, Color clanColor, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(Math.max(1.2, size * 0.15));
        if (clanColor.equals(RAN_RED)) {
            gc.strokeOval(mx - size / 2, my - size / 2, size, size);
        } else if (clanColor.equals(RAN_BLUE)) {
            gc.strokeLine(mx - size * 0.45, my, mx + size * 0.45, my);
        } else if (clanColor.equals(RAN_YELLOW)) {
            double h = size * 0.866;
            gc.strokePolygon(new double[] {mx, mx - size / 2, mx + size / 2},
                    new double[] {my - h / 2, my + h / 2, my + h / 2}, 3);
        } else {
            gc.strokeOval(mx - size / 2, my - size / 2, size, size);
        }
    }

    @Override public void onVisualizationReset() { super.onVisualizationReset(); clear(); }

    @Override
    public void dispose() {
        if (isDisposed()) return;
        canvas.setOnMouseClicked(null);
        canvas.widthProperty().unbind();
        canvas.heightProperty().unbind();
        super.dispose();
    }
}
