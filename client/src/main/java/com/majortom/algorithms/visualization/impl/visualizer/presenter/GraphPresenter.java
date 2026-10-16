package com.majortom.algorithms.visualization.impl.visualizer.presenter;

import com.majortom.algorithms.visualization.impl.visualizer.graph.GraphVisualText;
import com.majortom.algorithms.visualization.render.api.PresentationRenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderIntent;
import com.majortom.algorithms.visualization.render.api.RenderSessionId;
import com.majortom.algorithms.visualization.render.api.StructuralRenderIntent;
import com.majortom.algorithms.visualization.render.api.StructurePresenter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.runtime.graph.GraphViewState;
import java.util.Objects;

/**
 * 图的纯呈现策略：把前后 GraphViewState 比较转为 RenderIntent。
 *
 * <p>节点/边拓扑、方向、影响尺寸的边标签变更需要 StructuralRenderIntent；
 * 仅访问标记等瞬态观察变化使用 PresentationRenderIntent，
 * 避免每次算法事件都调用 ELK 重排整张图。
 *
 * <p>这里不拥有 JavaFX Node/摄像机，也不进行布局计算；
 * DefaultRenderFramework 决定异步布局、相机策略与 FX 提交。
 */
public final class GraphPresenter implements StructurePresenter<GraphViewState> {
    @Override
    public RenderIntent present(RenderSessionId sessionId, GraphViewState previous, GraphViewState current) {
        boolean initial = previous == null;
        if (!initial && !requiresStructuralLayout(previous, current)) {
            return new PresentationRenderIntent<>(sessionId, current);
        }
        return new StructuralRenderIntent<>(sessionId, current,
                initial ? CameraPolicy.RESTORE : CameraPolicy.ENSURE_VISIBLE, initial);
    }

    private static boolean requiresStructuralLayout(GraphViewState previous, GraphViewState current) {
        if (previous == null || previous.direction() != current.direction() || !previous.nodes().equals(current.nodes())) {
            return true;
        }
        // Observation-only frames retain the immutable edge list. Comparing
        // every edge and formatting its weight on each frame is unnecessary.
        if (previous.edges() == current.edges()) return false;
        if (previous.edges().size() != current.edges().size()) return true;
        for (int index = 0; index < previous.edges().size(); index++) {
            GraphViewState.Edge left = previous.edges().get(index);
            GraphViewState.Edge right = current.edges().get(index);
            if (left.id() != right.id() || left.fromId() != right.fromId() || left.toId() != right.toId()) return true;
            // Weight labels contribute measured decoration geometry, even when topology is unchanged.
            if (!Objects.equals(GraphVisualText.edgeLabel(left), GraphVisualText.edgeLabel(right))) return true;
        }
        return false;
    }
}
