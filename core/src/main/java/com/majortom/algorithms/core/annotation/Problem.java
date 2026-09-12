package com.majortom.algorithms.core.annotation;

import com.majortom.algorithms.core.problem.ProblemDifficulty;
import com.majortom.algorithms.core.problem.ProblemSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Problem {
    ProblemSource source();
    String id();
    String name() default "";
    String number() default "";
    ProblemDifficulty difficulty() default ProblemDifficulty.UNKNOWN;
    String[] tags() default {};
}
