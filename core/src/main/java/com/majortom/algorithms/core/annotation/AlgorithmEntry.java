package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记算法类唯一的执行入口。
 *
 * <p>算法发现器只允许一个 {@code @AlgorithmEntry}，并检查其为 public、
 * 非 static 且仅接收一个带 {@link Structure} 的结构契约参数。
 * 真正的调用由注册描述符完成，因此必须走框架入口才能获得执行事件、
 * 暂停/单步、失败处理以及可选的 {@link AlgorithmInitialize} 生命周期。
 *
 * <p>不要在这里处理 UI、线程调度或 JSON 文件加载；这些工作分别由客户端调度
 * 和算法侧的初始化方法负责。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AlgorithmEntry {}
