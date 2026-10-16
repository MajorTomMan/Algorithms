package com.majortom.algorithms.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional preparation step invoked immediately before {@link AlgorithmEntry}.
 *
 * <p>A class annotated with {@link Algorithm} may declare at most one
 * {@code @AlgorithmInit} method. It must be public, non-static, return void,
 * and accept the same Structure parameter (including generic arguments) as
 * its {@code @AlgorithmEntry} method.
 *
 * <p>The framework invokes both methods once per algorithm run on the same
 * algorithm instance and passes the same structure object. If preparation
 * fails, the entry method is not invoked.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AlgorithmInit {}
