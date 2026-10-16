package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/**
 * 标识业务层稳定键（例如 City.id），供注解式图映射关联顶点。
 *
 * <p>同一对象实例优先按引用识别；需要跨实例匹配时，
 * 图映射器使用“实体运行时类型 + {@code @ValueId} 值”索引。
 * 因而不同 City 对象只要类型和业务 ID 相同，仍可解析到同一顶点。
 *
 * <p><b>注意：</b>业务 ID 和 {@code Vertex.id()} 是两套概念。
 * 内部顶点、边、快照及事件都使用结构生成的 long ID；
 * 不要把业务 ID 当成回放或可视化中的顶点 ID。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ValueId {
}
