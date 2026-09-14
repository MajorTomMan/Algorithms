package com.majortom.algorithms.core.annotation;

import com.majortom.algorithms.core.metadata.StructureModule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares Structure metadata on a capability contract. Concrete registrations additionally
 * provide an id and implementation; metadata-only family contracts may omit both.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Structure {
    String id() default "";

    String name() default "";

    StructureModule module();

    Class<?> implementation() default Void.class;
}
