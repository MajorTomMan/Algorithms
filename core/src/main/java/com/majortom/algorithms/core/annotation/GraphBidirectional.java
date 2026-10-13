package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Boolean edge-data property; adds a reverse edge only in directed mode. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface GraphBidirectional {
}
