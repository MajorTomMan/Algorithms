package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Set;

public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    @Override
    protected void draw(StringViewState state) {
        clear();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) return;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        String text = state.target();
        if (text.isEmpty()) {
            gc.setFill(RAN_GRAY);
            gc.fillText("EMPTY STRING", width / 2, height / 2);
            return;
        }

        int columns = Math.max(1, Math.min(text.length(), (int) Math.max(1, width / 42.0)));
        double cell = Math.min(38.0, Math.max(26.0, (width - 40.0) / columns));
        double startX = 20 + cell / 2;
        double startY = Math.max(50, height * 0.32);
        Set<Integer> matched = matchedIndexes(state);

        for (int i = 0; i < text.length(); i++) {
            int row = i / columns;
            int col = i % columns;
            double x = startX + col * cell;
            double y = startY + row * (cell + 18);
            Color fill = RAN_IRON;
            if (matched.contains(i)) fill = RAN_BLUE;
            if (i == state.targetIndex()) fill = state.phase() == StringViewState.Phase.FALLBACK ? RAN_RED : RAN_YELLOW;
            gc.setFill(fill);
            gc.fillRoundRect(x - cell * 0.42, y - cell * 0.42, cell * 0.84, cell * 0.84, 8, 8);
            gc.setStroke(RAN_WHITE.deriveColor(0, 1, 1, 0.55));
            gc.strokeRoundRect(x - cell * 0.42, y - cell * 0.42, cell * 0.84, cell * 0.84, 8, 8);
            gc.setFill(fill.equals(RAN_YELLOW) ? RAN_BLACK : RAN_WHITE);
            gc.fillText(String.valueOf(text.charAt(i)), x, y);
            gc.setFont(Font.font("Consolas", 10));
            gc.setFill(RAN_GRAY);
            gc.fillText(String.valueOf(i), x, y + cell * 0.63);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        }

        if (!state.pattern().isEmpty()) {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(RAN_WHITE);
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
            gc.fillText("PATTERN  " + state.pattern(), 20, 24);
        }
        drawTransientFeedbackOverlay();
    }

    private static Set<Integer> matchedIndexes(StringViewState state) {
        Set<Integer> indexes = new HashSet<>();
        int length = state.pattern().length();
        for (Integer start : state.matches()) {
            for (int offset = 0; offset < length; offset++) indexes.add(start + offset);
        }
        return indexes;
    }
}
