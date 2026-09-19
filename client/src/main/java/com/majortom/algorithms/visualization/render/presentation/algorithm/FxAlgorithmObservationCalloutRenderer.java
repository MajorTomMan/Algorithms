package com.majortom.algorithms.visualization.render.presentation.algorithm;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.PresentationRenderer;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.algorithm.AlgorithmObservationCallout;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** FX-only viewport hint. No structure node, reducer or camera is accessed. */
public final class FxAlgorithmObservationCalloutRenderer
    implements PresentationRenderer<AlgorithmObservationCallout> {
  private final VBox card;
  private final Label eyebrow = new Label();
  private final Label sequence = new Label();
  private final Label title = new Label();
  private final Label subject = new Label();
  private final Label detail = new Label();
  private FadeTransition transition;
  private String previousRun = "";
  private int previousIndex = -1;

  public FxAlgorithmObservationCalloutRenderer(VBox card) {
    this.card = Objects.requireNonNull(card, "card");
    eyebrow.getStyleClass().add("observation-callout-eyebrow");
    sequence.getStyleClass().add("observation-callout-sequence");
    title.getStyleClass().add("observation-callout-title");
    subject.getStyleClass().add("observation-callout-subject");
    detail.getStyleClass().add("observation-callout-detail");
    for (Label label : new Label[] {title, subject, detail}) {
      label.setWrapText(true);
      label.setMaxWidth(Double.MAX_VALUE);
    }
    card.getChildren().setAll(eyebrow, sequence, title, subject, detail);
    show(false);
  }

  @Override
  public CompletionStage<Void> commit(AlgorithmObservationCallout cue, RenderCommitContext context) {
    Objects.requireNonNull(cue, "cue");
    Objects.requireNonNull(context, "context");
    if (transition != null) {
      transition.stop();
      transition = null;
    }
    if (!cue.visible()) {
      show(false);
      card.setOpacity(1.0d);
      previousRun = cue.runId();
      previousIndex = cue.cursorIndex();
      return CompletableFuture.completedFuture(null);
    }
    String style = "observation-callout-" + cue.category();
    card.getStyleClass().removeIf(value -> value.startsWith("observation-callout-")
        && !value.equals("observation-callout"));
    card.getStyleClass().add(style);
    eyebrow.setText(I18N.text("label.algorithm.observation.title"));
    sequence.setText(String.format(java.util.Locale.ROOT, "#%04d", cue.sequence()));
    title.setText(cue.titleKey().isBlank() ? cue.titleFallback() : I18N.text(cue.titleKey()));
    subject.setText(cue.subject());
    subject.setManaged(!cue.subject().isBlank());
    subject.setVisible(!cue.subject().isBlank());
    detail.setText(cue.detail());
    detail.setManaged(!cue.detail().isBlank());
    detail.setVisible(!cue.detail().isBlank());
    show(true);
    // A single adjacent forward step animates. Seek, backward step and locale refresh do not.
    boolean forward = previousRun.equals(cue.runId())
        && cue.cursorIndex() == previousIndex + 1;
    if (forward) {
      transition = new FadeTransition(Duration.millis(230.0d), card);
      transition.setFromValue(0.50d);
      transition.setToValue(1.0d);
      transition.play();
    } else {
      card.setOpacity(1.0d);
    }
    previousRun = cue.runId();
    previousIndex = cue.cursorIndex();
    return CompletableFuture.completedFuture(null);
  }

  private void show(boolean enabled) {
    card.setManaged(enabled);
    card.setVisible(enabled);
  }
}
