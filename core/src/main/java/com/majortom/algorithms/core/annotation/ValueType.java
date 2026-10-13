package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/** Marks a user value type and the structure contracts allowed to display it. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ValueType {
  Class<?>[] value() default {};
  String id() default "";
  String name() default "";
  /** Explicit developer guarantee: the complete reachable value is immutable. */
  boolean immutable() default false;
}
