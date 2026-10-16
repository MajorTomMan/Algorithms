/**
 * 结构视图的渲染控制平面，不拥有领域事件或算法真值。
 * 
 *  <p>StructureRenderDriver 提交 RenderIntent；DefaultRenderFramework
 *  按 session/revision 仲裁布局、相机和 FX 提交；
 *  RenderMailbox 提供最新状态合并与有序帧背压；
 *  LayoutCoordinator 在后台选择/复用布局引擎。
 * 
 *  <p>后台布局不得改 JavaFX Node；实际 UI 操作须经过 FxExecutor。
 *  呈现优化只影响画面，不能删除 ExecutionRuntime 的事实事件。
 */
package com.majortom.algorithms.visualization.render.runtime;
