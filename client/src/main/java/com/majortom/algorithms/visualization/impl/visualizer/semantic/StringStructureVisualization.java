package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.fx.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral string layout semantics. */
public final class StringStructureVisualization implements StructureVisualization<StringViewState> {
    @Override
    public LayoutRequest captureLayout(StringViewState state, RenderCaptureContext context) {
        int size = state.value().length();
        VisualDensity density = densityFor(size);
        double baseWidth = switch (density) {
            case DETAIL -> 52.0d;
            case COMPACT -> 40.0d;
            case DENSE -> 28.0d;
        };
        double height = Math.max(78.0d, 62.0d + context.contentStyle().fontSize());
        List<LayoutElement> elements = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String value = Character.toString(state.value().charAt(index));
            double width = DetachedMetrics.boxWidth(value, context.contentStyle(), baseWidth, 16.0d);
            elements.add(new LayoutElement(elementId(index), width, height));
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), LinearLayoutEngine.ID, elements,
                Map.of("structure", "string", "direction", "RIGHT", "padding", "28", "spacing", "0"));
    }

    public static String elementId(int index) { return "string:" + index; }

    private static VisualDensity densityFor(int size) {
        if (size <= 24) return VisualDensity.DETAIL;
        if (size <= 48) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }
}
