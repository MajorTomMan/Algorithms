package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

/**
 * 执行时间轴中的单帧快照。
 *
 * <p>算法每次调用 {@link ExecutionContext#sync(BaseStructure, Object, Object, String)}
 * 都会生成一个 frame。可视化层读取 {@link #snapshot()} 绘制结构状态，
 * 读取 {@link #focusA()} 和 {@link #focusB()} 高亮当前操作对象，
 * 读取 {@link #stats()} 更新统计面板。</p>
 *
 * @param index 帧序号，从 0 开始递增
 * @param timestampOffsetMillis 相对本次执行开始时间的偏移毫秒数
 * @param label 当前步骤说明，通常显示在日志或回放面板
 * @param focusA 第一个高亮焦点
 * @param focusB 第二个高亮焦点
 * @param snapshot 当前结构快照
 * @param stats 当前统计快照
 * @param <S> 快照结构类型
 */
public record ExecutionFrame<S extends BaseStructure<?>>(
        long index,
        long timestampOffsetMillis,
        String label,
        Object focusA,
        Object focusB,
        S snapshot,
        ExecutionStatsSnapshot stats) {
}
