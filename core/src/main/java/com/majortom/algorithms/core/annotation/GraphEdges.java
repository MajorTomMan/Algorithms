package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** A batch-data member containing typed edge records. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphEdges {
}
