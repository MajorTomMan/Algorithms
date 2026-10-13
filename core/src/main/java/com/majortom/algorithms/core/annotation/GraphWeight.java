package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Numeric edge-data property; defaults to 1.0 when omitted. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphWeight {
}
