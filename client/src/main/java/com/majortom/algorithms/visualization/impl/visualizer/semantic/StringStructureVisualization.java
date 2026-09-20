package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.render.api.LinearLayoutDirection;
import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.common.VisualDensity;
import com.majortom.algorithms.visualization.common.VisualDensityPolicy;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringVisualIds;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
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
        VisualDensity density = VisualDensityPolicy.string(size);
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
                Map.of(LayoutMetadataKeys.STRUCTURE, StructureIds.STRING,
                        LayoutMetadataKeys.DIRECTION, LinearLayoutDirection.RIGHT.name(),
                        LayoutMetadataKeys.PADDING, "28", LayoutMetadataKeys.SPACING, "0"));
    }

    public static String elementId(int index) { return StringVisualIds.node(index); }

}
