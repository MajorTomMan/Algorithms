package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.impl.visualizer.array.ArrayVisualIds;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.DetachedMetrics;
import com.majortom.algorithms.visualization.render.layout.LinearLayoutEngine;
import com.majortom.algorithms.visualization.runtime.array.ArrayViewState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral array layout semantics. */
public final class ArrayStructureVisualization implements StructureVisualization<ArrayViewState> {
    @Override
    public LayoutRequest captureLayout(ArrayViewState state, RenderCaptureContext context) {
        int size = state.values().size();
        VisualDensity density = densityFor(size);
        List<LayoutElement> elements = new ArrayList<>(size);
        double baseWidth = switch (density) {
            case DETAIL -> 64.0d;
            case COMPACT -> 50.0d;
            case DENSE -> 38.0d;
        };
        double height = Math.max(78.0d, 63.0d + context.contentStyle().fontSize());
        for (int index = 0; index < size; index++) {
            String text = state.values().get(index).text();
            double width = DetachedMetrics.boxWidth(text, context.contentStyle(), baseWidth, 20.0d);
            elements.add(new LayoutElement(elementId(index), width, height));
        }
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), LinearLayoutEngine.ID, elements, Map.of(LayoutMetadataKeys.STRUCTURE, StructureIds.ARRAY));
    }

    public static String elementId(int index) { return ArrayVisualIds.node(index); }

    private static VisualDensity densityFor(int size) {
        if (size <= 16) return VisualDensity.DETAIL;
        if (size <= 40) return VisualDensity.COMPACT;
        return VisualDensity.DENSE;
    }
}
