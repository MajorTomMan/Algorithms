package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.List;

/**
 * 一次完整算法运行的记录。
 *
 * <p>{@link ExecutionContext#finish()} 会生成这个对象。它把模块、算法、输入签名、
 * 时间轴、统计摘要和运行消息打包在一起，供回放、比较和导出使用。</p>
 *
 * @param moduleId 模块 ID
 * @param algorithmId 算法 ID
 * @param inputSignature 输入签名
 * @param createdAtMillis 记录创建时间
 * @param timeline 完整执行时间轴
 * @param summary 运行统计摘要
 * @param messages 运行消息列表
 * @param <S> 记录中的结构快照类型
 */
public record ExecutionRecord<S extends BaseStructure<?>>(
        String moduleId,
        String algorithmId,
        String inputSignature,
        long createdAtMillis,
        ExecutionTimeline<S> timeline,
        ExecutionStatsSnapshot summary,
        List<ExecutionMessage> messages) {
}
