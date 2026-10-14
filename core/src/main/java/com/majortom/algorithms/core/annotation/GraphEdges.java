package com.majortom.algorithms.core.annotation;

import java.lang.annotation.*;

/** A collection of edge objects. Endpoint and optional weight properties belong to each edge. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphEdges {
    String from() default "from";
    String to() default "to";
    String weight() default "";
    String bidirectional() default "";
}