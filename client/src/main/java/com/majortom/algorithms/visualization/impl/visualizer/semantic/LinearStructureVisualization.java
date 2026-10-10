package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.runtime.linked.LinearStructureViewState;
import com.majortom.algorithms.visualization.impl.visualizer.linear.LinearStructureLayoutSpec;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JavaFX-neutral deterministic layout semantics for stack/queue families. */
public final class LinearStructureVisualization implements StructureVisualization<LinearStructureViewState> {
    private final LinearStructureLayoutSpec spec;

    public LinearStructureVisualization(LinearStructureLayoutSpec spec) {
        this.spec = Objects.requireNonNull(spec, "spec");
    }

    @Override
    public LayoutRequest captureLayout(LinearStructureViewState state, RenderCaptureContext context) {
        List<LayoutElement> elements = new ArrayList<>(state.values().size());
        for (int index = 0; index < state.values().size(); index++) {
            String text = state.values().get(index).text();
            double width = DetachedMetrics.boxWidth(
                    text, context.contentStyle(), spec.minWidth(), spec.horizontalPadding());
            elements.add(new LayoutElement(elementId(spec.structure(), index), width, spec.height()));
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), LinearLayoutEngine.ID, elements,
                Map.of(LayoutMetadataKeys.STRUCTURE, spec.structure(),
                        LayoutMetadataKeys.DIRECTION, spec.direction().name(),
                        LayoutMetadataKeys.PADDING, number(spec.layoutPadding()),
                        LayoutMetadataKeys.SPACING, "0"));
    }

    public static String elementId(String structure, int index) {
        return structure + ":" + index;
    }

    private static String number(double value) {
        long integral = (long) value;
        return value == integral ? Long.toString(integral) : Double.toString(value);
    }
}
