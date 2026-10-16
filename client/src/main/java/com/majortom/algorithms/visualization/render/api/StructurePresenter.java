package com.majortom.algorithms.visualization.render.api;

/**
 * JavaFX-neutral policy that turns successive structure snapshots into framework render intents.
 * The presenter decides structural vs. presentational work and camera policy; FX renderers do not.
 */
@FunctionalInterface
/**
 * 由前后不可变视图状态产生渲染意图的纯策略接口。
 *
 * <p>算法/结构事件先被 EventReducer 投影成 ViewState；Presenter 再决定
 * 当前变更是 Structural（需要布局/相机）还是 Presentation（只更新可见状态）。
 * 例如 GraphPresenter 对仅“访问顶点”的变化不会重新触发 ELK。
 *
 * <p>不读写 JavaFX Node、不开线程；实际调度与提交归 RenderFramework。
 */
public interface StructurePresenter<S> {
    RenderIntent present(RenderSessionId sessionId, S previous, S current);
}
