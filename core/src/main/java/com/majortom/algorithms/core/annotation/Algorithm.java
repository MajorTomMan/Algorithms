package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 算法的声明式注册入口，作用类似框架中的组件清单。
 *
 * <p><b>职责：</b>只描述“这个算法是什么、接受什么结构与元素类型”，不执行算法，
 * 也不决定 UI 如何展示。算法发现器读取此元数据，再由注册表保存可调用的描述符。
 * 客户端根据 {@link #structure()} 和 {@link #types()} 筛选当前工作台可用算法。
 *
 * <p><b>调用链：</b>类扫描 → 算法描述符 → 客户端选择 → 执行框架。
 * 一个实现必须暴露唯一的 {@link AlgorithmEntry}；可选的
 * {@link AlgorithmInitialize} 在每次入口执行前先运行。
 *
 * <p><b>约束：</b>{@link #id()} 是稳定的 kebab-case 注册键；
 * {@link #name()} 仅用于显示，不参与类型匹配。泛型结构的参数顺序必须与
 * {@link AlgorithmEntry} 方法的泛型签名一致。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Algorithm {
  String id();

  String name() default "";

  /** Ordered runtime type signature. Generic Structure algorithms must match parameter order. */
  Class<?>[] types();

  /** Structure capability that determines compatibility and workbench module. */
  Class<?> structure();
}
