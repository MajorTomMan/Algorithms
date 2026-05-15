package com.majortom.algorithms.visualization.module;

import com.majortom.algorithms.visualization.BaseController;

import java.util.function.Supplier;

/**
 * 可视化模块定义。
 *
 * <p>主界面不直接依赖具体 Controller 构造逻辑，而是读取这个记录：
 * {@link #id()} 用于内部识别，{@link #labelKey()} 用于国际化显示，
 * {@link #controllerFactory()} 用于按需创建模块控制器。</p>
 *
 * @param id 模块稳定 ID，例如 sort、tree、graph、maze
 * @param labelKey 国际化资源 key
 * @param controllerFactory 控制器工厂，每次调用应返回一个可独立运行的新控制器
 */
public record AlgorithmModuleDefinition(
        String id,
        String labelKey,
        Supplier<BaseController<?>> controllerFactory) {
}
