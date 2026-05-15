package com.majortom.algorithms.core.runtime;

/**
 * 执行消息等级。
 *
 * <p>用于给 {@link ExecutionMessage} 分类，UI 日志或导出工具可以根据等级调整展示方式。</p>
 */
public enum ExecutionMessageLevel {
    /**
     * 普通信息。
     */
    INFO,

    /**
     * 警告信息。
     */
    WARN,

    /**
     * 错误信息。
     */
    ERROR
}
