package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.common.AnimationCoordinator;
import com.majortom.algorithms.visualization.common.ViewportPane;
import com.majortom.algorithms.visualization.impl.visualizer.string.StringCellView;
import com.majortom.algorithms.visualization.runtime.string.StringViewState;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure JavaFX String renderer using a deterministic horizontal character layout. */
public final class StringVisualizer extends BaseVisualizer<StringViewState> {
    private static final double START_X = 36.0d;
    private static final double START_Y = 64.0d;
    private static final double CELL_STEP = 58.0d;
    private static final Duration APPEAR_DURATION = Duration.millis(150.0d);
    private static final Duration DISAPPEAR_DURATION = Duration.millis(120.0d);

    private final ViewportPane viewport = new ViewportPane();
    private final AnimationCoordinator animations = new AnimationCoordinator();
    private final Map<Integer, StringCellView> cells = new LinkedHashMap<>();
    private final Text emptyLabel = new Text("EMPTY STRING");
    private Animation activeAnimation;
    private boolean firstRender = true;

    public StringVisualizer() {
        getChildren().setAll(viewport);
        viewport.prefWidthProperty().bind(widthProperty());
        viewport.prefHeightProperty().bind(heightProperty());
        emptyLabel.getStyleClass().add("visual-empty-label");
    }

    @Override
    protected void draw(StringViewState state) {
        stopActiveAnimation();
        List<Animation> transitions = new ArrayList<>();
        String value = state.value();

        if (value.isEmpty()) {
            clearCells(transitions);
            if (!viewport.content().getChildren().contains(emptyLabel)) {
                emptyLabel.relocate(START_X, START_Y);
                viewport.content().getChildren().add(emptyLabel);
            }
            play(transitions);
            firstRender = false;
            return;
        }
        viewport.content().getChildren().remove(emptyLabel);

        for (int index = 0; index < value.length(); index++) {
            StringCellView cell = cells.get(index);
            boolean added = false;
            if (cell == null) {
                cell = new StringCellView(index, value.charAt(index));
                cells.put(index, cell);
                viewport.content().getChildren().add(cell);
                added = true;
            }
            cell.setIndex(index);
            cell.setValue(value.charAt(index));
            cell.setHighlighted(isMutationIndex(state.mutation(), index));
            cell.setCompleted(state.completed());
            cell.relocate(START_X + index * CELL_STEP, START_Y);
            if (added && !firstRender) {
                transitions.add(animations.together(
                        animations.fadeIn(cell, APPEAR_DURATION),
                        animations.scaleIn(cell, APPEAR_DURATION)));
            }
        }

        List<Integer> removed = cells.keySet().stream().filter(index -> index >= value.length()).toList();
        for (Integer index : removed) {
            StringCellView cell = cells.remove(index);
            if (firstRender) {
                viewport.content().getChildren().remove(cell);
            } else {
                Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
                fade.setOnFinished(event -> viewport.content().getChildren().remove(cell));
                transitions.add(fade);
            }
        }

        if (firstRender) {
            firstRender = false;
            viewport.fitToViewport();
            return;
        }
        play(transitions);
    }

    private boolean isMutationIndex(StringViewState.Mutation mutation, int index) {
        if (mutation.type() == StringViewState.Type.NONE || mutation.index() < 0) {
            return false;
        }
        int end = mutation.index() + Math.max(1, mutation.length());
        return index >= mutation.index() && index < end;
    }

    private void clearCells(List<Animation> transitions) {
        if (firstRender) {
            cells.clear();
            viewport.content().getChildren().clear();
            return;
        }
        for (StringCellView cell : cells.values()) {
            Animation fade = animations.fadeOut(cell, DISAPPEAR_DURATION);
            fade.setOnFinished(event -> viewport.content().getChildren().remove(cell));
            transitions.add(fade);
        }
        cells.clear();
    }

    private void play(List<Animation> transitions) {
        if (transitions.isEmpty()) {
            return;
        }
        ParallelTransition parallel = new ParallelTransition();
        parallel.getChildren().addAll(transitions);
        activeAnimation = parallel;
        parallel.play();
    }

    private void stopActiveAnimation() {
        if (activeAnimation != null) {
            activeAnimation.stop();
            activeAnimation = null;
        }
    }

    @Override
    public void setPlaybackSpeed(double speed) {
        animations.setPlaybackSpeed(speed);
    }

    @Override
    public void setScrubbing(boolean scrubbing) {
        animations.setScrubbing(scrubbing);
    }

    @Override
    public void onVisualizationReset() {
        stopActiveAnimation();
        cells.clear();
        viewport.content().getChildren().clear();
        firstRender = true;
        viewport.reset();
    }

    @Override
    public void dispose() {
        stopActiveAnimation();
        viewport.prefWidthProperty().unbind();
        viewport.prefHeightProperty().unbind();
        super.dispose();
    }
}
