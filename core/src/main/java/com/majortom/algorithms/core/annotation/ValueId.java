package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Optional stable business key; never replaces the internal vertex ID. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ValueId {
}
