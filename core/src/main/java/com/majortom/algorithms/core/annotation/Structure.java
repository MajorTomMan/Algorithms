package com.majortom.algorithms.core.annotation;

import com.majortom.algorithms.core.metadata.StructureModule;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明数据结构的能力契约，以及可选的具体实现注册信息。
 *
 * <p>结构接口只暴露算法需要的操作，算法依赖接口而不是具体结构实现。
 * 带完整 {@link #id()} 与 {@link #implementation()} 的接口可由结构发现器注册，
 * 再由注册表按契约构造结构。两者必须同时提供；两者都为空时可作为
 * 仅描述所属 {@link #module()} 的能力家族接口。
 *
 * <p><b>边界：</b>结构层维护真实状态与不变量，不能依赖 JavaFX；
 * 算法声明的结构契约决定兼容性，工作台模块使用 {@link #module()} 分类。
 * {@link #name()} 是显示名称，不替代稳定 ID。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Structure {
  String id() default "";

  String name() default "";

  StructureModule module();

  Class<?> implementation() default Void.class;
}
