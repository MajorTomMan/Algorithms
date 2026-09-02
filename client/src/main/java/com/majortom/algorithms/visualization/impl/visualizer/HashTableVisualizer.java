package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.impl.controller.HashTableViewState;
import javafx.geometry.VPos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public final class HashTableVisualizer extends BaseVisualizer<HashTableViewState> {
    @Override
    protected void draw(HashTableViewState state) {
        clear();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        if (width <= 0 || height <= 0) return;
        HashTableSnapshot<String, Integer> snapshot = state.table();
        List<List<HashTableSnapshot.Entry<String, Integer>>> buckets = buckets(snapshot);
        double rowHeight = Math.max(34, Math.min(52, (height - 30) / Math.max(1, snapshot.capacity())));
        double bucketWidth = Math.min(64, Math.max(46, width * 0.12));
        double entryWidth = Math.min(110, Math.max(76, width * 0.18));
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        for (int index = 0; index < snapshot.capacity(); index++) {
            double y = 18 + index * rowHeight;
            gc.setFill(RAN_IRON);
            gc.fillRoundRect(18, y, bucketWidth, rowHeight * 0.68, 8, 8);
            gc.setStroke(RAN_GRAY);
            gc.strokeRoundRect(18, y, bucketWidth, rowHeight * 0.68, 8, 8);
            gc.setFill(RAN_WHITE);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("[" + index + "]", 18 + bucketWidth / 2, y + rowHeight * 0.34);
            double x = 18 + bucketWidth + 24;
            for (HashTableSnapshot.Entry<String, Integer> entry : buckets.get(index)) {
                boolean focus = entry.key().equals(state.focusKey());
                gc.setFill(focus ? (state.found() ? RAN_BLUE : RAN_RED) : RAN_ASH);
                gc.fillRoundRect(x, y, entryWidth, rowHeight * 0.68, 8, 8);
                gc.setStroke(focus ? RAN_YELLOW : RAN_GRAY);
                gc.strokeRoundRect(x, y, entryWidth, rowHeight * 0.68, 8, 8);
                gc.setFill(RAN_WHITE);
                gc.fillText(entry.key() + " = " + entry.value(), x + entryWidth / 2, y + rowHeight * 0.34);
                x += entryWidth + 22;
                if (x > width - entryWidth) break;
            }
        }
    }

    private static List<List<HashTableSnapshot.Entry<String, Integer>>> buckets(HashTableSnapshot<String, Integer> snapshot) {
        List<List<HashTableSnapshot.Entry<String, Integer>>> result = new ArrayList<>();
        for (int i = 0; i < snapshot.capacity(); i++) result.add(new ArrayList<>());
        for (HashTableSnapshot.Entry<String, Integer> entry : snapshot.entries()) {
            if (entry.bucketIndex() < result.size()) result.get(entry.bucketIndex()).add(entry);
        }
        return result;
    }
}
