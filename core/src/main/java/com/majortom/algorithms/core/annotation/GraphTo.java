package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Marks the target endpoint of an edge-data record. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphTo {
}
