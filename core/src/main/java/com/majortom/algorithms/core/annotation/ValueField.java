package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/**
 * 声明业务对象中允许展示的字段，而不是图的拓扑结构。
 *
 * <p>元数据层按 {@link #order()} 和字段名稳定排序；
 * SUMMARY 供节点标签、列表等紧凑视图使用，DETAIL 只在详情检查时读取，
 * 避免渲染每一帧时执行昂贵的 getter。{@link #label()} 用于界面文案，
 * 不参与 JSON 反序列化或实体关联。
 *
 * <p>未标注的属性不会因为反射扫描而自动暴露到 UI。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ValueField {
  ValueDisplay[] value() default {ValueDisplay.DETAIL};
  String label() default "";
  int order() default 0;
}
