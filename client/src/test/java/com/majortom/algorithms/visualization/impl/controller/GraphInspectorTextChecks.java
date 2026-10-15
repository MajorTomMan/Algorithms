package com.majortom.algorithms.visualization.impl.controller;

import java.util.Locale;
import java.util.ResourceBundle;

/** Verifies that graph edge identity and business values use distinct inspector rows. */
public final class GraphInspectorTextChecks {
    public static void main(String[] args) {
        check(Locale.CHINESE, new String[] { "边 ID", "起点 ID", "起点值", "终点 ID", "终点值", "权重", "有向" }, "否");
        check(Locale.ENGLISH, new String[] { "Edge ID", "From ID", "From value", "To ID", "To value", "Weight", "Directed" }, "No");
        System.out.println("GraphInspectorTextChecks passed");
    }

    private static void check(Locale locale, String[] labels, String no) {
        String pattern = ResourceBundle.getBundle("language.language", locale)
                .getString("label.workspace.selection.graph.edge.detail");
        String result = String.format(locale, pattern, 72, 3, "2", 4, "3", "12", no);
        String[] lines = result.split("\\R");
        String[] values = { "#72", "#3", "2", "#4", "3", "12", no };
        if (lines.length != labels.length)
            throw new AssertionError("Expected seven inspector rows, got " + lines.length);
        for (int i = 0; i < labels.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith(labels[i]) || !line.endsWith(values[i]) || line.contains(" · "))
                throw new AssertionError("Wrong inspector row: " + line);
        }
    }
}
