package com.majortom.algorithms.visualization.common;

import com.majortom.algorithms.visualization.common.view.EdgeView;
import com.majortom.algorithms.visualization.common.view.NodeView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

/** Thin speed-aware visual animation helper. Runtime/Timeline remain the authoritative event history. */
public final class AnimationCoordinator {
    private double playbackSpeed = 1.0d;
    private boolean scrubbing;

    public double playbackSpeed() {
        return playbackSpeed;
    }

    public void setPlaybackSpeed(double playbackSpeed) {
        if (playbackSpeed <= 0.0d) {
            throw new IllegalArgumentException("playbackSpeed must be positive");
        }
        this.playbackSpeed = playbackSpeed;
    }

    public boolean isScrubbing() {
        return scrubbing;
    }

    public void setScrubbing(boolean scrubbing) {
        this.scrubbing = scrubbing;
    }

    public Animation move(NodeView node, Point2D target, Duration baseDuration) {
        Duration duration = effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            node.setCenter(target.getX(), target.getY());
            return new PauseTransition(Duration.ZERO);
        }
        return new Timeline(new KeyFrame(
                duration,
                new KeyValue(node.centerXProperty(), target.getX()),
                new KeyValue(node.centerYProperty(), target.getY())));
    }

    public Animation fadeIn(Node node, Duration baseDuration) {
        Duration duration = effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            node.setOpacity(1.0d);
            return new PauseTransition(Duration.ZERO);
        }
        node.setOpacity(0.0d);
        FadeTransition transition = new FadeTransition(duration, node);
        transition.setToValue(1.0d);
        return transition;
    }

    public Animation scaleIn(Node node, Duration baseDuration) {
        Duration duration = effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            node.setScaleX(1.0d);
            node.setScaleY(1.0d);
            return new PauseTransition(Duration.ZERO);
        }
        node.setScaleX(0.65d);
        node.setScaleY(0.65d);
        ScaleTransition transition = new ScaleTransition(duration, node);
        transition.setToX(1.0d);
        transition.setToY(1.0d);
        return transition;
    }

    public Animation fadeOut(Node node, Duration baseDuration) {
        Duration duration = effectiveDuration(baseDuration);
        if (duration.lessThanOrEqualTo(Duration.ZERO)) {
            node.setOpacity(0.0d);
            return new PauseTransition(Duration.ZERO);
        }
        FadeTransition transition = new FadeTransition(duration, node);
        transition.setToValue(0.0d);
        return transition;
    }

    public Animation reveal(EdgeView edge, Duration baseDuration) {
        return fadeIn(edge, baseDuration);
    }

    public Animation together(Animation... animations) {
        return new ParallelTransition(animations);
    }

    public Duration effectiveDuration(Duration baseDuration) {
        if (baseDuration == null || baseDuration.lessThanOrEqualTo(Duration.ZERO) || scrubbing || playbackSpeed >= 16.0d) {
            return Duration.ZERO;
        }
        return baseDuration.divide(playbackSpeed);
    }
}
