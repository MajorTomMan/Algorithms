package com.majortom.algorithms.visualization.algorithm;

import com.majortom.algorithms.core.base.BaseAlgorithms;

import java.util.function.Supplier;

/**
 * 可视化层的算法注册项。
 *
 * <p>它描述一个算法如何出现在 UI 中，以及被选中时如何创建新的算法实例。
 * 注册表每次通过 {@link #create()} 创建新实例，避免多次运行共享算法内部状态。</p>
 *
 * @param id 执行记录和调试日志中使用的稳定算法 ID
 * @param labelKey UI 显示名称的国际化 key
 * @param moduleId 所属模块 ID，例如 {@code sort} 或 {@code maze}
 * @param family 算法用途
 * @param structure 适配的数据结构类型
 * @param factory 算法实例工厂
 */
public record AlgorithmDescriptor(
        String id,
        String labelKey,
        String moduleId,
        AlgorithmFamily family,
        AlgorithmStructure structure,
        Supplier<? extends BaseAlgorithms> factory) {

    /**
     * 创建一个新的算法实例。
     *
     * @return 新算法实例
     */
    @SuppressWarnings("unchecked")
    public BaseAlgorithms<?> create() {
        return (BaseAlgorithms<?>) factory.get();
    }
}
