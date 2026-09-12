package com.majortom.algorithms.practice.runtime.annotation;

import com.majortom.algorithms.practice.runtime.model.ProblemSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Problem {
    ProblemSource source();
    String id();
    String title();
}
