package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.impl.controller.LinearStructureViewState;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral deterministic layout semantics for stack/queue families. */
public final class LinearStructureVisualization implements StructureVisualization<LinearStructureViewState> {
    private final String structure;
    private final String direction;
    private final double minWidth;
    private final double height;
    private final double horizontalPadding;
    private final double layoutPadding;

    public LinearStructureVisualization(String structure, String direction, double minWidth,
            double height, double horizontalPadding, double layoutPadding) {
        this.structure = structure;
        this.direction = direction;
        this.minWidth = minWidth;
        this.height = height;
        this.horizontalPadding = horizontalPadding;
        this.layoutPadding = layoutPadding;
    }

    @Override
    public LayoutRequest captureLayout(LinearStructureViewState state, RenderCaptureContext context) {
        List<LayoutElement> elements = new ArrayList<>(state.values().size());
        for (int index = 0; index < state.values().size(); index++) {
            String text = state.values().get(index).text();
            double width = DetachedMetrics.boxWidth(
                    text, context.contentStyle(), minWidth, horizontalPadding);
            elements.add(new LayoutElement(elementId(structure, index), width, height));
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), LinearLayoutEngine.ID, elements,
                Map.of("structure", structure, "direction", direction,
                        "padding", number(layoutPadding), "spacing", "0"));
    }

    public static String elementId(String structure, int index) { return structure + ":" + index; }

    private static String number(double value) {
        long integral = (long) value;
        return value == integral ? Long.toString(integral) : Double.toString(value);
    }
}
