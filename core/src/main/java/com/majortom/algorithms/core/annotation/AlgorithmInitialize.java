package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记每次算法运行前的可选数据准备阶段。
 *
 * <p>生命周期顺序：创建算法实例 → {@code @AlgorithmInitialize} →
 * {@link AlgorithmEntry}。同一次运行中的两个方法使用同一个算法实例和同一个结构对象；
 * 初始化失败时不会进入算法入口。没有此注解的算法按旧流程直接执行。
 *
 * <p><b>签名约束：</b>一个算法最多声明一个初始化方法；它必须为 public、
 * 非 static、返回 void，且参数（包含泛型）与 {@link AlgorithmEntry} 完全一致。
 * 相关规则在发现/注册阶段校验，而非运行到一半才发现类型错误。
 *
 * <p><b>事件与线程：</b>初始化由算法描述符在同一次执行上下文中调用，
 * 初始化过程中对结构的修改可产生结构事件，并进入客户端的可视化回放。
 * 这里不是“应用启动一次”的初始化器，每次运行都会调用；业务方应考虑幂等和覆盖策略。
 *
 * <p>注意：直接手动调用算法的 {@code execute()} 方法不会触发此注解。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AlgorithmInitialize {}
