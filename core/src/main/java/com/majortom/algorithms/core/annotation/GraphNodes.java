package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/**
 * 标识从容器或业务对象中递归收集顶点的成员。
 *
 * <p>图映射器读取此成员返回的单值、Iterable、数组、Optional 或 Map 的值，
 * 将其作为候选节点继续遍历，并使用对象身份集合阻断循环引用。
 * 此注解只声明“哪些数据是顶点”，不负责创建 UI 节点。
 *
 * <p>与 {@link GraphEdges} 配对使用，由 Graph.from(...) 将模型转换为图快照。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphNodes {
}