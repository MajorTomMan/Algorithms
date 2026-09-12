package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a discoverable Algorithm implementation and its runtime registration metadata. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Algorithm {
    /** Stable algorithm id within the {@code (module, type)} namespace. */
    String id();

    /** Human-readable component name. Blank uses the implementation class name fallback. */
    String name() default "";

    /** Workbench/domain grouping such as {@code array}, {@code graph} or {@code maze}. */
    String module();

    /** Runtime value type handled by this algorithm registration. */
    Class<?> type();

    /** Required Structure capability contract; {@link Void} means no generic Structure contract. */
    Class<?> structure() default Void.class;
}
