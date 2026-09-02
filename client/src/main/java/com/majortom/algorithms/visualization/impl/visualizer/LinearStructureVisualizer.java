package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public final class LinearStructureVisualizer extends BaseVisualizer<LinearStructureViewState> {
    @Override
    protected void draw(LinearStructureViewState state) {
        clear();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font(14));
        if (state.values().isEmpty()) {
            gc.setFill(RAN_GRAY);
            gc.fillText(state.kind(), width / 2.0, height / 2.0);
            return;
        }
        int count = state.values().size();
        double cellWidth = Math.min(72.0, Math.max(34.0, (width - 48.0) / count));
        double totalWidth = cellWidth * count;
        double startX = Math.max(24.0, (width - totalWidth) / 2.0);
        double y = height / 2.0 - 24.0;
        for (int i = 0; i < count; i++) {
            double x = startX + i * cellWidth;
            gc.setStroke(RAN_BLUE);
            gc.setLineWidth(2.0);
            gc.strokeRect(x, y, cellWidth - 6.0, 48.0);
            gc.setFill(RAN_WHITE);
            gc.fillText(String.valueOf(state.values().get(i)), x + (cellWidth - 6.0) / 2.0, y + 29.0);
            if (i + 1 < count) {
                gc.setStroke(Color.web("#70757a"));
                gc.strokeLine(x + cellWidth - 5.0, y + 24.0, x + cellWidth + 1.0, y + 24.0);
            }
        }
        gc.setFill(RAN_GRAY);
        gc.fillText(state.kind(), width / 2.0, y - 20.0);
    }
}
