package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/**
 * 声明可被工作台发现的业务值类型，避免为每个实体手动注册 UI 类型。
 *
 * <p>客户端在算法包中发现此注解后，为实体提供默认 JSON 文本适配器；
 * {@link #name()} 仅控制选择器显示（例如“城市”），底层算法仍以 Class
 * 身份进行类型匹配，不以中文名称或 {@link #id()} 字符串匹配。
 *
 * <p>{@link #value()} 列出允许使用该实体的 {@link Structure} 能力契约；
 * 为空表示不额外限制。结构选择器和数据结构的类型边界都应遵守此限制。
 *
 * <p>{@link #immutable()} 是开发者对整个可达对象图不可变的承诺，
 * 客户端据此决定是否可以保存并回放事件中的值。仅使用 Java record
 * 并不保证内部 List、Map 或其元素不可变。
 *
 * <p><b>不负责：</b>此注解不声明数据文件路径、不自动加载 JSON，
 * 也不解决跨文件实体 ID 的业务关联。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ValueType {
  Class<?>[] value() default {};
  String id() default "";
  String name() default "";
  /** Explicit developer guarantee: the complete reachable value is immutable. */
  boolean immutable() default false;
}
