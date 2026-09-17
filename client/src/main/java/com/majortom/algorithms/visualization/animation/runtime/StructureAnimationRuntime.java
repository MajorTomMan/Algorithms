package com.majortom.algorithms.visualization.animation.runtime;

import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationPlanner;
import com.majortom.algorithms.visualization.animation.fx.AnimationSceneAdapter;
import com.majortom.algorithms.visualization.animation.fx.FxAnimationPlayer;
import com.majortom.algorithms.visualization.render.api.LayoutPatch;
import java.util.Objects;

/**
 * Commit-boundary animation coordinator. It remembers only the previous committed facts/layout and
 * delegates transition derivation to a JavaFX-neutral planner and execution to the FX player.
 */
public final class StructureAnimationRuntime<S> implements AnimationControl {
  private final AnimationPlanner<S> planner;
  private final FxAnimationPlayer player = new FxAnimationPlayer();
  private S previousState;
  private LayoutPatch previousLayout;
  private boolean disposed;

  public StructureAnimationRuntime(AnimationPlanner<S> planner) {
    this.planner = Objects.requireNonNull(planner, "planner");
  }

  /**
   * Starts a new factual transition. The renderer must call this before mutating its active FX maps,
   * so an interrupted animation can capture the currently visible positions first.
   */
  public AnimationPlan beginTransition(S nextState, LayoutPatch nextLayout, boolean animate) {
    Objects.requireNonNull(nextState, "nextState");
    Objects.requireNonNull(nextLayout, "nextLayout");
    if (disposed) return AnimationPlan.empty();

    AnimationPlan plan = AnimationPlan.empty();
    if (animate && previousState != null && previousLayout != null) {
      player.interruptForTransition();
      plan = planner.plan(previousState, previousLayout, nextState, nextLayout);
    } else {
      player.finishImmediately();
    }
    previousState = nextState;
    previousLayout = nextLayout;
    return plan;
  }

  public void play(AnimationPlan plan, AnimationSceneAdapter scene) {
    if (disposed) {
      scene.stabilize(plan);
      return;
    }
    player.play(Objects.requireNonNull(plan, "plan"), Objects.requireNonNull(scene, "scene"));
  }

  @Override
  public void setSpeed(double speed) {
    player.setSpeed(speed);
  }

  @Override
  public void setScrubbing(boolean scrubbing) {
    player.setScrubbing(scrubbing);
  }

  @Override
  public void pause() {
    player.pause();
  }

  @Override
  public void resume() {
    player.resume();
  }

  @Override
  public void finishImmediately() {
    player.finishImmediately();
  }

  @Override
  public void reset() {
    previousState = null;
    previousLayout = null;
    player.reset();
  }

  @Override
  public void dispose() {
    if (disposed) return;
    disposed = true;
    previousState = null;
    previousLayout = null;
    player.dispose();
  }

  @Override
  public boolean isAnimating() {
    return player.isAnimating();
  }
}
