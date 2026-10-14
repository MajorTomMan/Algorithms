package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Marks child vertices, recursively followed on every discovered node. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphNodes {
}