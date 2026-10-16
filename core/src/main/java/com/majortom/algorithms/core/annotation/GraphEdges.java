package com.majortom.algorithms.core.annotation;

import java.lang.annotation.*;

/**
 * 标识一组业务边，并指定每个边对象上的端点、权重和双向属性。
 *
 * <p>元数据层解析 {@link #from()} / {@link #to()} 指向的 Java 成员；
 * 图映射层再将端点对象与 {@link GraphNodes} 收集的顶点关联。
 * {@link #weight()}、{@link #bidirectional()} 为空时使用默认值，
 * 声明的成员必须能被元数据层正确读取和转换。
 *
 * <p>在有向图上，双向道路会被展开成两条不同边 ID 的有向边；
 * 在无向图上，不需要另外建立反向边。这是一对多边图，不应按端点去重。
 *
 * <p><b>不负责：</b>此注解既不解析 JSON 文件，也不自动把字符串
 * 城市 ID 映射成 City 对象；跨文件引用必须先转换成映射器可识别的端点。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphEdges {
    String from() default "from";
    String to() default "to";
    String weight() default "";
    String bidirectional() default "";
}