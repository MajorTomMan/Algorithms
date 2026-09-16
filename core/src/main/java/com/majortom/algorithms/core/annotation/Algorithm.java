package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a discoverable Algorithm implementation and its Structure-driven registration metadata.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Algorithm {
    String id();

    String name() default "";

    Class<?> type();

    /** Structure capability that determines compatibility and workbench module. */
    Class<?> structure();
}
