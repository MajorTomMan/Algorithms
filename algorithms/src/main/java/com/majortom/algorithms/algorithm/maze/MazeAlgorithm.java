package com.majortom.algorithms.algorithm.maze;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Maze-domain metadata used only to classify algorithms for maze presentation. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MazeAlgorithm {
  MazeRole role();

  MazeModel model() default MazeModel.ARRAY;
}
