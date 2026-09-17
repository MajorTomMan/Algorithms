package com.majortom.algorithms.visualization.impl.visualizer.semantic;

import com.majortom.algorithms.visualization.render.api.LayoutMetadataKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.render.api.LayoutElement;
import com.majortom.algorithms.visualization.render.api.LayoutRequest;
import com.majortom.algorithms.visualization.render.api.StructureVisualization;
import com.majortom.algorithms.visualization.render.api.RenderCaptureContext;
import com.majortom.algorithms.visualization.render.layout.FixedLayoutEngine;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import java.util.List;
import java.util.Map;

/** JavaFX-neutral maze world extent semantics. */
public final class MazeStructureVisualization implements StructureVisualization<MazeViewState> {
    private static final double WORLD_CELL_SIZE = 32.0d;
    public static final String GRID_ID = "maze:grid";

    @Override
    public LayoutRequest captureLayout(MazeViewState state, RenderCaptureContext context) {
        List<LayoutElement> elements = state.rows() < 1 || state.columns() < 1
                ? List.of()
                : List.of(new LayoutElement(GRID_ID,
                        state.columns() * WORLD_CELL_SIZE, state.rows() * WORLD_CELL_SIZE));
        return new LayoutRequest(context.requestId(), context.sessionId(), context.modelRevision(),
                context.geometryRevision(), FixedLayoutEngine.ID, elements, Map.of(LayoutMetadataKeys.STRUCTURE, StructureIds.MAZE));
    }
}
