package com.majortom.algorithms.core.runtime;

/**
 * 算法执行过程中产生的消息。
 *
 * <p>消息会被 {@link ExecutionContext#message(ExecutionMessageLevel, String, String)}
 * 收集，并随 {@link ExecutionRecord} 一起用于 UI 日志、导出或调试。</p>
 *
 * @param timestampMillis 消息产生时的系统时间戳
 * @param level 消息等级
 * @param code 稳定消息编码，便于后续国际化或筛选
 * @param text 消息文本
 */
public record ExecutionMessage(
        long timestampMillis,
        ExecutionMessageLevel level,
        String code,
        String text) {
}
