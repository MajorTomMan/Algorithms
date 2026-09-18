package com.majortom.algorithms.visualization.memory;

import com.majortom.algorithms.core.memory.MemorySample;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

import java.util.List;

/** Compact cumulative-allocation plot for the memory inspector. */
public final class MemoryAllocationChart extends Pane {
    private static final double LEFT_INSET = 10.0d;
    private static final double RIGHT_INSET = 10.0d;
    private static final double TOP_INSET = 10.0d;
    private static final double BOTTOM_INSET = 10.0d;

    private final Line topGrid = gridLine();
    private final Line middleGrid = gridLine();
    private final Line bottomGrid = gridLine();
    private final Path curve = new Path();
    private final Rectangle clip = new Rectangle();
    private List<MemorySample> samples = List.of();

    public MemoryAllocationChart() {
        getStyleClass().add("memory-allocation-chart");
        curve.getStyleClass().add("memory-allocation-curve");
        curve.setMouseTransparent(true);
        getChildren().addAll(topGrid, middleGrid, bottomGrid, curve);
        setClip(clip);
    }

    public void setSamples(List<MemorySample> samples) {
        this.samples = samples == null ? List.of() : List.copyOf(samples);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(0.0d, getWidth());
        double height = Math.max(0.0d, getHeight());
        clip.setWidth(width);
        clip.setHeight(height);

        double x0 = LEFT_INSET;
        double x1 = Math.max(x0, width - RIGHT_INSET);
        double y0 = TOP_INSET;
        double y1 = Math.max(y0, height - BOTTOM_INSET);
        double middle = y0 + (y1 - y0) * 0.5d;
        positionGrid(topGrid, x0, x1, y0);
        positionGrid(middleGrid, x0, x1, middle);
        positionGrid(bottomGrid, x0, x1, y1);

        curve.getElements().clear();
        if (samples.isEmpty() || x1 <= x0 || y1 <= y0) {
            return;
        }
        long maxElapsed = Math.max(1L, samples.getLast().elapsedNanos());
        long maxAllocated = 1L;
        for (MemorySample sample : samples) {
            maxAllocated = Math.max(maxAllocated, sample.allocatedBytes());
        }
        for (int index = 0; index < samples.size(); index++) {
            MemorySample sample = samples.get(index);
            double x = x0 + ((double) sample.elapsedNanos() / maxElapsed) * (x1 - x0);
            double y = y1 - ((double) sample.allocatedBytes() / maxAllocated) * (y1 - y0);
            if (index == 0) {
                curve.getElements().add(new MoveTo(x, y));
            } else {
                curve.getElements().add(new LineTo(x, y));
            }
        }
    }

    private static Line gridLine() {
        Line line = new Line();
        line.getStyleClass().add("memory-chart-grid-line");
        line.setMouseTransparent(true);
        return line;
    }

    private static void positionGrid(Line line, double x0, double x1, double y) {
        line.setStartX(x0);
        line.setEndX(x1);
        line.setStartY(y);
        line.setEndY(y);
    }
}
