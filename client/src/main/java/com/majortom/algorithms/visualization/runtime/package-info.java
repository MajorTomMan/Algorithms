/**
 * JavaFX 客户端的运行会话、事件投影与回放层。
 * 
 *  <p>LocalAlgorithmExecution 把同步的 core.ExecutionRuntime 安排在 worker，
 *  保留完整权威事件，并使用独立观察通道向 UI 交付通知。
 *  EventReducer 把事件纯归约成不可变视图；ReducedEventTimeline
 *  选择可见帧，PlaybackController 只调度回放而不重新执行算法。
 * 
 *  <p>暂停算法线程与暂停动画回放是两种独立操作；
 *  真正场景提交在 render 包完成。
 */
package com.majortom.algorithms.visualization.runtime;
