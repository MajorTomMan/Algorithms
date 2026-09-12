package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares one concrete Structure implementation for a capability contract. */
@Documented
@Repeatable(Structures.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Structure {
    /** Stable id used when an implementation must be selected explicitly. */
    String id();

    /** Human-readable component name. Blank uses the implementation class name fallback. */
    String name() default "";

    /** Structure capability/API contract implemented by the annotated class. */
    Class<?> contract();
}
