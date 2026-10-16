package com.majortom.algorithms.core.annotation;
import java.lang.annotation.*;
/**
 * Declares a business value type for automatic Workbench discovery.
 *
 * <p>{@link #name()} is the visible display name (e.g. "城市"),
 * while Java Class identity remains the registration key. {@link #value()}
 * restricts the Structure contracts in which the type may be selected.
 * Empty value() means all structures.
 */
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
