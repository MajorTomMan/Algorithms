package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Declares explicitly visible properties, independent of graph topology. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface ValueField {
  ValueDisplay[] value() default {ValueDisplay.DETAIL};
  String label() default "";
  int order() default 0;
}
