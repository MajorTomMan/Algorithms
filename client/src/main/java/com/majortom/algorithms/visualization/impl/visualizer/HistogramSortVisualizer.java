package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.runtime.sort.IntegerSortViewState;
import javafx.animation.AnimationTimer;
import javafx.scene.effect.Bloom;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Animated clan-flag renderer for immutable integer-sort projections. */
public final class HistogramSortVisualizer extends BaseVisualizer<IntegerSortViewState> {

    private static final long FRAME_INTERVAL_NANOS = 33_000_000L;

    private final Bloom focusBloom = new Bloom(0.3d);
    private final AnimationTimer windTimer;
    private double wavePhase;
    private long lastFrameNanos;
    private boolean windRunning;
    private boolean animationRequested = true;
    private double valueLabelSlotWidth = -1.0d;
    private Font valueLabelFont;

    public HistogramSortVisualizer() {
        windTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastFrameNanos < FRAME_INTERVAL_NANOS) {
                    return;
                }
                lastFrameNanos = now;
                wavePhase += 0.075d;
                drawCurrent();
            }
        };
    }

    @Override
    protected void draw(IntegerSortViewState state) {
        clear();
        if (state.values().isEmpty()) {
            stopWind();
            return;
        }

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double sidePadding = 40.0d;
        double horizonY = Math.max(80.0d, height - 100.0d);
        double availableHeight = Math.max(25.0d, height - 180.0d);
        int maximum = state.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double barWidth = Math.max(1.0d, (width - sidePadding * 2.0d) / state.values().size());
        Font labelFont = valueLabelFont(barWidth);

        for (int index = 0; index < state.values().size(); index++) {
            int value = state.values().get(index);
            double flagHeight = Math.max(25.0d, value * availableHeight / Math.max(1, maximum));
            double x = sidePadding + index * barWidth;
            boolean focused = index == state.comparedIndex() || index == state.insertionIndex()
                    || index == state.swapLeftIndex() || index == state.swapRightIndex()
                    || index == state.pivotIndex();
            renderClanFlag(
                    x, horizonY, barWidth, flagHeight, colorFor(state, index), focused, index, value, labelFont);
        }

        updateWindState();
    }

    private Color colorFor(IntegerSortViewState state, int index) {
        if (state.completed()) {
            return RAN_EMERALD;
        }
        if (state.settledIndices().contains(index)) {
            return RAN_EMERALD;
        }
        if (index == state.swapLeftIndex() || index == state.swapRightIndex()) {
            return RAN_VIOLET;
        }
        if (index == state.pivotIndex()) {
            return RAN_GOLD;
        }
        if (index == state.insertionIndex()) {
            return RAN_BLUE;
        }
        if (index == state.comparedIndex()) {
            return RAN_YELLOW;
        }
        if (state.rangeStart() >= 0 && index >= state.rangeStart() && index <= state.rangeEnd()) {
            return RAN_RED.interpolate(RAN_GOLD, 0.24d);
        }
        return RAN_RED;
    }

    private void renderClanFlag(
            double x,
            double horizonY,
            double width,
            double height,
            Color color,
            boolean focused,
            int index,
            int value,
            Font labelFont) {
        double poleX = x + width / 2.0d;
        double topY = horizonY - height;
        double flagLength = width * 1.1d;
        if (width > 10.0d) {
            flagLength = width * 1.5d;
        }
        double flagHeight = Math.min(height * 0.5d, 45.0d);
        double wave = Math.sin(wavePhase + index * 0.5d) * flagLength * 0.08d;

        gc.save();
        if (focused) {
            gc.setEffect(focusBloom);
        }
        gc.setStroke(RAN_WHITE.deriveColor(0.0d, 1.0d, 1.0d, 0.24d));
        gc.setLineWidth(Math.max(0.6d, width * 0.1d));
        gc.strokeLine(poleX, horizonY, poleX, topY);

        gc.beginPath();
        gc.moveTo(poleX, topY);
        gc.lineTo(poleX - flagLength + wave, topY + flagHeight / 2.0d);
        gc.lineTo(poleX, topY + flagHeight);
        gc.closePath();
        gc.setFill(new LinearGradient(
                0.0d, 0.0d, 1.0d, 0.0d, true, CycleMethod.NO_CYCLE,
                new Stop(0.0d, color.deriveColor(0.0d, 1.0d, 0.9d, 0.95d)),
                new Stop(1.0d, color.deriveColor(0.0d, 0.8d, 1.2d, 0.6d))));
        gc.fill();

        if (width > 15.0d) {
            double monX = poleX - flagLength * 0.4d + wave * 0.3d;
            drawClanMon(monX, topY + flagHeight / 2.0d, flagHeight * 0.35d,
                    color, RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.55d));
        }
        if (focused) {
            gc.setStroke(RAN_WHITE);
            gc.setLineWidth(1.2d);
            gc.strokeOval(poleX - 4.0d, topY - 4.0d, 8.0d, 8.0d);
        }
        drawFlagValue(poleX, horizonY, width, value, focused, labelFont);
        gc.restore();
    }

    private void drawFlagValue(
            double poleX,
            double horizonY,
            double flagSlotWidth,
            int value,
            boolean focused,
            Font labelFont) {
        if (labelFont == null) {
            return;
        }

        String label = Integer.toString(value);
        double fontSize = labelFont.getSize();
        double labelWidth = label.length() * fontSize * 0.62d;
        if (labelWidth + 2.0d > flagSlotWidth * 0.86d) {
            return;
        }

        double labelY = horizonY - 8.0d;
        gc.setFont(labelFont);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
        gc.setLineWidth(Math.max(1.0d, fontSize * 0.13d));
        gc.setStroke(RAN_BLACK.deriveColor(0.0d, 1.0d, 1.0d, 0.88d));
        gc.strokeText(label, poleX, labelY);
        Color labelColor = RAN_GOLD;
        if (focused) {
            labelColor = RAN_WHITE;
        }
        gc.setFill(labelColor);
        gc.fillText(label, poleX, labelY);
    }

    private Font valueLabelFont(double flagSlotWidth) {
        if (flagSlotWidth < 11.0d) {
            return null;
        }
        if (valueLabelFont == null || Math.abs(flagSlotWidth - valueLabelSlotWidth) > 0.25d) {
            double fontSize = Math.min(14.0d, Math.max(8.0d, flagSlotWidth * 0.44d));
            valueLabelFont = Font.font("Consolas", FontWeight.BOLD, fontSize);
            valueLabelSlotWidth = flagSlotWidth;
        }
        return valueLabelFont;
    }

    @Override
    public void setPlaybackPaused(boolean paused) {
        animationRequested = !paused;
        updateWindState();
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
        updateWindState();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        stopWind();
        resetLocalState();
        super.onModuleDetached(moduleId);
        clear();
    }

    @Override
    protected void onResizeStateChanged(boolean resizing) {
        if (resizing) {
            stopWind();
            return;
        }
        updateWindState();
    }

    @Override
    public void dispose() {
        stopWind();
        super.dispose();
    }

    private void updateWindState() {
        IntegerSortViewState state = currentState();
        boolean hasData = state != null && !state.values().isEmpty();
        if (animationRequested && isModuleAttached() && !isResizeInProgress() && hasData && !isDisposed()) {
            startWind();
        } else {
            stopWind();
        }
    }

    private void startWind() {
        if (windRunning) {
            return;
        }
        windRunning = true;
        lastFrameNanos = 0L;
        windTimer.start();
    }

    private void stopWind() {
        if (!windRunning) {
            return;
        }
        windTimer.stop();
        windRunning = false;
    }

    private void resetLocalState() {
        stopWind();
        wavePhase = 0.0d;
    }
}
