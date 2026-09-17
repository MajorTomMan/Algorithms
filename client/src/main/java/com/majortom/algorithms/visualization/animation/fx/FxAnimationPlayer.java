package com.majortom.algorithms.visualization.animation.fx;

import com.majortom.algorithms.visualization.animation.api.AnimationControl;
import com.majortom.algorithms.visualization.animation.api.AnimationPlan;
import com.majortom.algorithms.visualization.animation.api.AnimationStep;
import com.majortom.algorithms.visualization.animation.api.AnimationTimings;
import com.majortom.algorithms.visualization.animation.api.TimedAnimationStep;
import com.majortom.algorithms.visualization.common.view.EdgeView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.util.Duration;

/** Single JavaFX animation execution point for structure-transition plans. */
public final class FxAnimationPlayer implements AnimationControl {
    private static final double ENTER_SCALE = 0.84d;
    private static final double EXIT_SCALE = 0.86d;
    private static final double VALUE_PULSE_SCALE = 1.075d;
    private static final double EPSILON = 0.0001d;

    private Timeline timeline;
    private AnimationSceneAdapter currentScene;
    private AnimationPlan currentPlan = AnimationPlan.empty();
    private double speed = 1.0d;
    private boolean scrubbing;
    private boolean paused;
    private boolean stepRequested;
    private boolean disposed;

    /**
     * Interrupts a running transition without snapping authoritative visuals to their old target.
     * Exit-only transient objects are discarded because they are already absent from factual state.
     */
    public void interruptForTransition() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (currentScene != null) currentScene.discardExitedVisuals();
        currentScene = null;
        currentPlan = AnimationPlan.empty();
    }

    /** Executes a prepared plan. The call itself is non-blocking and never owns Render scheduling. */
    public void play(AnimationPlan plan, AnimationSceneAdapter scene) {
        if (disposed) {
            scene.stabilize(plan);
            return;
        }
        currentScene = scene;
        currentPlan = plan;
        if (scrubbing || plan.isEmpty()) {
            finishImmediately();
            return;
        }

        Timeline next = new Timeline();
        Set<String> explicitNodes = new HashSet<>();
        Set<String> explicitEdges = new HashSet<>();

        // Preserve visual continuity after the factual commit moved nodes to final LayoutPatch positions.
        for (AnimationSceneAdapter.NodeTarget target : scene.activeNodes()) {
            String logicalId = target.logicalId();
            scene.capturedNodeCenter(logicalId).ifPresent(start -> {
                Point2D finalCenter = target.finalCenter();
                target.node().setTranslateX(start.getX() - finalCenter.getX());
                target.node().setTranslateY(start.getY() - finalCenter.getY());
            });
        }

        for (TimedAnimationStep timed : plan.steps()) {
            AnimationStep step = timed.step();
            if (step instanceof AnimationStep.NodeMove move) {
                explicitNodes.add(move.targetId());
                scene.node(move.targetId()).ifPresent(target -> animateNodeMove(next, timed, target));
            } else if (step instanceof AnimationStep.NodeEnter enter) {
                explicitNodes.add(enter.targetId());
                scene.node(enter.targetId()).ifPresent(target -> animateNodeEnter(next, timed, target));
            } else if (step instanceof AnimationStep.NodeExit exit) {
                explicitNodes.add(exit.targetId());
                scene.node(exit.targetId()).ifPresent(target -> animateNodeExit(next, timed, target));
            } else if (step instanceof AnimationStep.EdgeCreate create) {
                explicitEdges.add(create.targetId());
                scene.edge(create.targetId()).ifPresent(target -> animateEdgeCreate(next, timed, target.edge()));
            } else if (step instanceof AnimationStep.EdgeRemove remove) {
                explicitEdges.add(remove.targetId());
                scene.edge(remove.targetId()).ifPresent(target -> animateEdgeRemove(next, timed, target.edge()));
            } else if (step instanceof AnimationStep.ValueChange valueChange) {
                scene.node(valueChange.targetId()).ifPresent(target -> animateValueChange(next, timed, target));
            } else if (step instanceof AnimationStep.EdgeMorph morph) {
                explicitEdges.add(morph.targetId());
                scene.edge(morph.targetId()).ifPresent(target -> animateEdgeMorph(next, timed, scene, target));
            }
        }

        // An interrupted enter/move may still be visually mid-flight even when the new factual diff
        // no longer contains that primitive. Always settle retained authoritative visuals smoothly.
        for (AnimationSceneAdapter.NodeTarget target : scene.activeNodes()) {
            String logicalId = target.logicalId();
            if (explicitNodes.contains(logicalId)) continue;
            if (needsNodeSettle(target.node())) {
                TimedAnimationStep settle = new TimedAnimationStep(
                        new AnimationStep.NodeMove(logicalId), 0.0d, AnimationTimings.INTERRUPTED_SETTLE_MS);
                animateNodeMove(next, settle, target);
                tween(next, target.node().opacityProperty(), 0.0d, settle.durationMillis(),
                        target.node().getOpacity(), 1.0d, Interpolator.EASE_OUT);
                tween(next, target.node().scaleXProperty(), 0.0d, settle.durationMillis(),
                        target.node().getScaleX(), 1.0d, Interpolator.EASE_OUT);
                tween(next, target.node().scaleYProperty(), 0.0d, settle.durationMillis(),
                        target.node().getScaleY(), 1.0d, Interpolator.EASE_OUT);
            }
        }
        for (AnimationSceneAdapter.EdgeTarget target : scene.activeEdges()) {
            String logicalId = target.logicalId();
            EdgeView edge = target.edge();
            if (explicitEdges.contains(logicalId)) continue;
            if (edge.getRevealProgress() < 1.0d - EPSILON || edge.getOpacity() < 1.0d - EPSILON) {
                TimedAnimationStep settle = new TimedAnimationStep(
                        new AnimationStep.EdgeMorph(logicalId), 0.0d, AnimationTimings.INTERRUPTED_SETTLE_MS);
                animateEdgeSettle(next, settle, edge);
            }
        }

        if (next.getKeyFrames().isEmpty()) {
            finishImmediately();
            return;
        }
        next.setRate(speed);
        next.setOnFinished(event -> {
            if (timeline != next) return;
            timeline = null;
            AnimationSceneAdapter finishedScene = currentScene;
            AnimationPlan finishedPlan = currentPlan;
            currentScene = null;
            currentPlan = AnimationPlan.empty();
            if (finishedScene != null) finishedScene.stabilize(finishedPlan);
        });
        timeline = next;
        boolean playOneTransition = stepRequested;
        stepRequested = false;
        next.play();
        if (paused && !playOneTransition) next.pause();
    }

    private void animateNodeMove(Timeline timeline, TimedAnimationStep timed, AnimationSceneAdapter.NodeTarget target) {
        Node node = target.node();
        tween(timeline, node.translateXProperty(), timed.startMillis(), timed.durationMillis(),
                node.getTranslateX(), 0.0d, Interpolator.EASE_BOTH);
        tween(timeline, node.translateYProperty(), timed.startMillis(), timed.durationMillis(),
                node.getTranslateY(), 0.0d, Interpolator.EASE_BOTH);
        // A new structural transition may interrupt an enter/exit pulse. Retained nodes must
        // converge to the stable presentation while their positional transition continues.
        tween(timeline, node.opacityProperty(), timed.startMillis(), timed.durationMillis(),
                node.getOpacity(), 1.0d, Interpolator.EASE_OUT);
        tween(timeline, node.scaleXProperty(), timed.startMillis(), timed.durationMillis(),
                node.getScaleX(), 1.0d, Interpolator.EASE_OUT);
        tween(timeline, node.scaleYProperty(), timed.startMillis(), timed.durationMillis(),
                node.getScaleY(), 1.0d, Interpolator.EASE_OUT);
        for (Node companion : target.companions()) {
            tween(timeline, companion.opacityProperty(), timed.startMillis(), timed.durationMillis(),
                    companion.getOpacity(), 1.0d, Interpolator.EASE_OUT);
        }
    }

    private void animateNodeEnter(Timeline timeline, TimedAnimationStep timed, AnimationSceneAdapter.NodeTarget target) {
        Node node = target.node();
        node.setOpacity(0.0d);
        node.setScaleX(ENTER_SCALE);
        node.setScaleY(ENTER_SCALE);
        target.companions().forEach(companion -> companion.setOpacity(0.0d));
        tween(timeline, node.opacityProperty(), timed.startMillis(), timed.durationMillis(), 0.0d, 1.0d, Interpolator.EASE_OUT);
        tween(timeline, node.scaleXProperty(), timed.startMillis(), timed.durationMillis(), ENTER_SCALE, 1.0d, Interpolator.EASE_OUT);
        tween(timeline, node.scaleYProperty(), timed.startMillis(), timed.durationMillis(), ENTER_SCALE, 1.0d, Interpolator.EASE_OUT);
        for (Node companion : target.companions()) {
            tween(timeline, companion.opacityProperty(), timed.startMillis(), timed.durationMillis(), 0.0d, 1.0d, Interpolator.EASE_OUT);
        }
    }

    private void animateNodeExit(Timeline timeline, TimedAnimationStep timed, AnimationSceneAdapter.NodeTarget target) {
        Node node = target.node();
        tween(timeline, node.opacityProperty(), timed.startMillis(), timed.durationMillis(), node.getOpacity(), 0.0d, Interpolator.EASE_IN);
        tween(timeline, node.scaleXProperty(), timed.startMillis(), timed.durationMillis(), node.getScaleX(), EXIT_SCALE, Interpolator.EASE_IN);
        tween(timeline, node.scaleYProperty(), timed.startMillis(), timed.durationMillis(), node.getScaleY(), EXIT_SCALE, Interpolator.EASE_IN);
        for (Node companion : target.companions()) {
            tween(timeline, companion.opacityProperty(), timed.startMillis(), timed.durationMillis(), companion.getOpacity(), 0.0d, Interpolator.EASE_IN);
        }
    }

    private void animateEdgeCreate(Timeline timeline, TimedAnimationStep timed, EdgeView edge) {
        edge.setRevealProgress(0.0d);
        edge.setOpacity(Math.min(edge.getOpacity(), 0.55d));
        tween(timeline, edge.revealProgressProperty(), timed.startMillis(), timed.durationMillis(), 0.0d, 1.0d, Interpolator.EASE_OUT);
        tween(timeline, edge.opacityProperty(), timed.startMillis(), timed.durationMillis(), edge.getOpacity(), 1.0d, Interpolator.EASE_OUT);
    }

    private void animateEdgeRemove(Timeline timeline, TimedAnimationStep timed, EdgeView edge) {
        tween(timeline, edge.revealProgressProperty(), timed.startMillis(), timed.durationMillis(), edge.getRevealProgress(), 0.0d, Interpolator.EASE_IN);
        tween(timeline, edge.opacityProperty(), timed.startMillis(), timed.durationMillis(), edge.getOpacity(), 0.15d, Interpolator.EASE_IN);
    }

    private void animateEdgeMorph(Timeline timeline, TimedAnimationStep timed,
            AnimationSceneAdapter scene, AnimationSceneAdapter.EdgeTarget target) {
        EdgeView edge = target.edge();
        List<Point2D> start = scene.capturedEdgeRoute(target.logicalId()).orElse(List.of());
        List<Point2D> end = edge.routeSnapshot();
        if (start.size() < 2 || end.size() < 2) {
            animateEdgeSettle(timeline, timed, edge);
            return;
        }

        int sampleCount = Math.max(2, Math.max(start.size(), end.size()));
        List<Point2D> sampledStart = resamplePolyline(start, sampleCount);
        List<Point2D> sampledEnd = resamplePolyline(end, sampleCount);
        DoubleProperty progress = new SimpleDoubleProperty(0.0d);
        progress.addListener((observable, previous, current) ->
                edge.setRoute(interpolatePolyline(sampledStart, sampledEnd, current.doubleValue())));
        edge.setRoute(sampledStart);
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(timed.startMillis()),
                new KeyValue(progress, 0.0d)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(timed.endMillis()),
                new KeyValue(progress, 1.0d, Interpolator.EASE_BOTH)));
        animateEdgeSettle(timeline, timed, edge);
    }

    private void animateEdgeSettle(Timeline timeline, TimedAnimationStep timed, EdgeView edge) {
        tween(timeline, edge.revealProgressProperty(), timed.startMillis(), timed.durationMillis(), edge.getRevealProgress(), 1.0d, Interpolator.EASE_OUT);
        tween(timeline, edge.opacityProperty(), timed.startMillis(), timed.durationMillis(), edge.getOpacity(), 1.0d, Interpolator.EASE_OUT);
    }

    private static List<Point2D> interpolatePolyline(List<Point2D> start, List<Point2D> end, double progress) {
        double t = Math.max(0.0d, Math.min(1.0d, progress));
        List<Point2D> result = new ArrayList<>(start.size());
        for (int index = 0; index < start.size(); index++) {
            Point2D left = start.get(index);
            Point2D right = end.get(index);
            result.add(left.add(right.subtract(left).multiply(t)));
        }
        return List.copyOf(result);
    }

    private static List<Point2D> resamplePolyline(List<Point2D> points, int sampleCount) {
        if (points.size() < 2 || sampleCount <= 2) {
            return List.of(points.getFirst(), points.getLast());
        }
        double total = 0.0d;
        double[] cumulative = new double[points.size()];
        for (int index = 1; index < points.size(); index++) {
            total += points.get(index - 1).distance(points.get(index));
            cumulative[index] = total;
        }
        if (total <= EPSILON) {
            return java.util.Collections.nCopies(sampleCount, points.getFirst());
        }
        List<Point2D> result = new ArrayList<>(sampleCount);
        int segment = 1;
        for (int sample = 0; sample < sampleCount; sample++) {
            double distance = total * sample / (sampleCount - 1.0d);
            while (segment < cumulative.length - 1 && cumulative[segment] < distance) segment++;
            Point2D left = points.get(segment - 1);
            Point2D right = points.get(segment);
            double segmentStart = cumulative[segment - 1];
            double segmentLength = cumulative[segment] - segmentStart;
            double fraction = segmentLength <= EPSILON ? 0.0d : (distance - segmentStart) / segmentLength;
            result.add(left.add(right.subtract(left).multiply(fraction)));
        }
        return List.copyOf(result);
    }

    private void animateValueChange(Timeline timeline, TimedAnimationStep timed, AnimationSceneAdapter.NodeTarget target) {
        Node node = target.node();
        double half = timed.startMillis() + timed.durationMillis() * 0.5d;
        double end = timed.endMillis();
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(timed.startMillis()),
                new KeyValue(node.scaleXProperty(), node.getScaleX()),
                new KeyValue(node.scaleYProperty(), node.getScaleY())));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(half),
                new KeyValue(node.scaleXProperty(), VALUE_PULSE_SCALE, Interpolator.EASE_OUT),
                new KeyValue(node.scaleYProperty(), VALUE_PULSE_SCALE, Interpolator.EASE_OUT)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(end),
                new KeyValue(node.scaleXProperty(), 1.0d, Interpolator.EASE_IN),
                new KeyValue(node.scaleYProperty(), 1.0d, Interpolator.EASE_IN)));
    }

    private static void tween(Timeline timeline, DoubleProperty property, double startMillis,
            double durationMillis, double from, double to, Interpolator interpolator) {
        if (Math.abs(from - to) < EPSILON) return;
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(startMillis), new KeyValue(property, from)));
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(startMillis + durationMillis),
                new KeyValue(property, to, interpolator)));
    }

    private static boolean needsNodeSettle(Node node) {
        return Math.abs(node.getTranslateX()) > EPSILON
                || Math.abs(node.getTranslateY()) > EPSILON
                || Math.abs(node.getOpacity() - 1.0d) > EPSILON
                || Math.abs(node.getScaleX() - 1.0d) > EPSILON
                || Math.abs(node.getScaleY() - 1.0d) > EPSILON;
    }

    @Override
    public void setSpeed(double speed) {
        if (!Double.isFinite(speed) || speed <= 0.0d) return;
        this.speed = Math.max(0.05d, Math.min(32.0d, speed));
        if (timeline != null) timeline.setRate(this.speed);
    }

    @Override
    public void setScrubbing(boolean scrubbing) {
        this.scrubbing = scrubbing;
        if (scrubbing) finishImmediately();
    }

    @Override
    public void pause() {
        paused = true;
        if (timeline != null) timeline.pause();
    }

    @Override
    public void resume() {
        paused = false;
        stepRequested = false;
        if (timeline != null) timeline.play();
    }

    /** Plays exactly one current/next transition while keeping global playback paused. */
    @Override
    public void step() {
        if (disposed) return;
        paused = true;
        if (timeline != null) {
            stepRequested = false;
            timeline.play();
        } else {
            stepRequested = true;
        }
    }

    @Override
    public void reset() {
        finishImmediately();
        scrubbing = false;
        paused = false;
        stepRequested = false;
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        stepRequested = false;
        finishImmediately();
    }

    @Override
    public boolean isAnimating() {
        return timeline != null;
    }

    @Override
    public void finishImmediately() {
        stepRequested = false;
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        AnimationSceneAdapter scene = currentScene;
        AnimationPlan plan = currentPlan;
        currentScene = null;
        currentPlan = AnimationPlan.empty();
        if (scene != null) scene.stabilize(plan);
    }
}
