package com.majortom.algorithms.visualization.render.api;


/**
 * JavaFX-neutral structure semantics for one visualization family.
 *
 * <p>This side only describes factual layout input. It never creates or mutates JavaFX nodes and
 * never owns viewport, camera, lifecycle or commit timing.</p>
 */
@FunctionalInterface
/**
 * 结构视图的纯布局语义描述接口，不负责 JavaFX Node 的生命周期。
 *
 * <p>把当前不可变 ViewState 和 RenderCaptureContext 变成 LayoutRequest；
 * RenderFramework 可以在非 FX 布局线程求解位置，
 * 并将结果交给 FxStructureRenderer 提交。
 *
 * <p>不可在实现中改动算法结构真值、事件时间线或 JavaFX UI；
 * 布局是同一事实状态的一种可替换投影。
 */
public interface StructureVisualization<S> {
    LayoutRequest captureLayout(S snapshot, RenderCaptureContext context);
}
