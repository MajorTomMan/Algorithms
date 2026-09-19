package com.majortom.algorithms.visualization.render.presentation.algorithm;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.PresentationRenderer;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import com.majortom.algorithms.visualization.runtime.algorithm.AlgorithmObservationModel;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Isolated FX commit for algorithm-only search/cache presentation; never touches structure nodes. */
public final class FxAlgorithmObservationRenderer
    implements PresentationRenderer<AlgorithmObservationModel> {
  private final VBox card;
  private final Label heading = new Label();
  private final Label searchTitle = new Label();
  private final Label searchTarget = value();
  private final Label searchCandidate = value();
  private final Label searchStatus = value();
  private final Label cacheTitle = new Label();
  private final Label cacheNotice = value();
  private final VBox cacheRows = new VBox(3.0d);
  private final Label[] recent = new Label[AlgorithmObservationModel.MAX_RECENT_CACHE];
  private final Label frontierTitle = new Label();
  private final Label frontierSummary = value();
  private final VBox frontierRows = new VBox(2.0d);
  private final Label[] candidateRows = new Label[4];
  private final VBox frontierSection = new VBox(4.0d);
  private final Label callTitle = new Label();
  private final Label callSummary = value();
  private final VBox callRows = new VBox(2.0d);
  private final Label[] frameRows = new Label[5];
  private final VBox callSection = new VBox(4.0d);
  private final Label pulse = value();
  private final VBox searchSection = new VBox(5.0d);
  private final VBox cacheSection = new VBox(5.0d);
  private FadeTransition transition;
  private String lastRun = "";
  private long lastSequence = -1L;

  public FxAlgorithmObservationRenderer(VBox card) {
    this.card = Objects.requireNonNull(card, "card");
    heading.getStyleClass().add("inspector-heading");
    searchTitle.getStyleClass().add("inspector-heading");
    cacheTitle.getStyleClass().add("inspector-heading");
    frontierTitle.getStyleClass().add("inspector-heading");
    callTitle.getStyleClass().add("inspector-heading");
    pulse.getStyleClass().add("event-kind");
    pulse.setWrapText(true);
    pulse.setMaxWidth(Double.MAX_VALUE);
    searchSection.getChildren().setAll(searchTitle, searchTarget, searchCandidate, searchStatus);
    for (int index = 0; index < recent.length; index++) {
      recent[index] = value();
      cacheRows.getChildren().add(recent[index]);
    }
    cacheSection.getChildren().setAll(cacheTitle, cacheNotice, cacheRows);
    for (int index = 0; index < candidateRows.length; index++) {
      candidateRows[index] = value();
      frontierRows.getChildren().add(candidateRows[index]);
    }
    for (int index = 0; index < frameRows.length; index++) {
      frameRows[index] = value();
      callRows.getChildren().add(frameRows[index]);
    }
    frontierSection.getChildren().setAll(frontierTitle, frontierSummary, frontierRows);
    callSection.getChildren().setAll(callTitle, callSummary, callRows);
    card.getChildren().setAll(heading, pulse, searchSection, cacheSection, frontierSection, callSection);
    visible(card, false);
  }

  @Override
  public CompletionStage<Void> commit(AlgorithmObservationModel model, RenderCommitContext context) {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(context, "context");
    if (transition != null) transition.stop();
    boolean next = model.hasContent();
    visible(card, next);
    if (!next) {
      lastRun = model.runId();
      lastSequence = model.sequence();
      return CompletableFuture.completedFuture(null);
    }
    heading.setText(I18N.text("label.algorithm.observation.title"));
    searchTitle.setText(I18N.text("label.algorithm.observation.search"));
    cacheTitle.setText(I18N.text("label.algorithm.observation.cache"));
    AlgorithmObservationModel.Search search = model.currentSearch();
    visible(searchSection, search != null);
    if (search != null) {
      searchTarget.setText(I18N.text("label.algorithm.observation.target") + "  " + search.target());
      searchCandidate.setText(I18N.text("label.algorithm.observation.candidate") + "  "
          + (search.candidate().isBlank() ? "—" : search.candidate()));
      searchStatus.setText(I18N.text("label.algorithm.observation.probes") + "  " + search.probes()
          + "  ·  " + I18N.text("label.algorithm.observation.results") + "  " + search.resultCount()
          + (search.completed() ? "  ·  " + I18N.text("label.algorithm.observation.complete") : ""));
    }
    List<AlgorithmObservationModel.CacheEntry> entries = model.recentCache();
    visible(cacheSection, !entries.isEmpty());
    cacheNotice.setText(I18N.text("label.algorithm.observation.recent"));
    for (int index = 0; index < recent.length; index++) {
      Label row = recent[index];
      visible(row, index < entries.size());
      if (index < entries.size()) {
        AlgorithmObservationModel.CacheEntry entry = entries.get(index);
        row.setText(entry.cacheId() + " / " + entry.key() + "  ·  " + kindText(entry.lastAction()));
      }
    }
    AlgorithmObservationModel.Frontier frontier = model.currentFrontier();
    visible(frontierSection, frontier != null);
    if (frontier != null) {
      frontierTitle.setText(I18N.text("label.algorithm.observation.frontier") + " · " + frontier.id());
      frontierSummary.setText(I18N.text("label.algorithm.observation.pending") + " "
          + frontier.count(AlgorithmObservationModel.CandidateStatus.PENDING) + "  ·  "
          + I18N.text("label.algorithm.observation.selected") + " "
          + frontier.count(AlgorithmObservationModel.CandidateStatus.SELECTED) + "  ·  "
          + I18N.text("label.algorithm.observation.pruned") + " "
          + frontier.count(AlgorithmObservationModel.CandidateStatus.PRUNED));
      List<AlgorithmObservationModel.Candidate> candidates = frontier.recentCandidates();
      int start = Math.max(0, candidates.size() - candidateRows.length);
      for (int index = 0; index < candidateRows.length; index++) {
        Label row = candidateRows[index];
        int candidateIndex = start + index;
        visible(row, candidateIndex < candidates.size());
        if (candidateIndex < candidates.size()) {
          AlgorithmObservationModel.Candidate candidate = candidates.get(candidateIndex);
          row.setText(candidate.id() + " · " + candidate.reference() + " · "
              + I18N.text("label.algorithm.observation.status_"
                  + candidate.status().name().toLowerCase(java.util.Locale.ROOT)));
        }
      }
    }
    List<AlgorithmObservationModel.CallFrame> frames = model.callStack();
    visible(callSection, !frames.isEmpty() || model.pulse().kind() == AlgorithmObservationModel.Kind.CALL_RETURNED
        || model.pulse().kind() == AlgorithmObservationModel.Kind.CALL_INVALID);
    callTitle.setText(I18N.text("label.algorithm.observation.call_stack"));
    callSummary.setText(I18N.text("label.algorithm.observation.depth") + " " + frames.size());
    int startFrame = Math.max(0, frames.size() - frameRows.length);
    for (int index = 0; index < frameRows.length; index++) {
      Label row = frameRows[index];
      int frameIndex = startFrame + index;
      visible(row, frameIndex < frames.size());
      if (frameIndex < frames.size()) {
        AlgorithmObservationModel.CallFrame frame = frames.get(frameIndex);
        row.setText("#" + (frameIndex + 1) + "  " + frame.label() + " · " + frame.id());
      }
    }
    AlgorithmObservationModel.Pulse current = model.pulse();
    visible(pulse, current.kind() != AlgorithmObservationModel.Kind.NONE);
    if (current.kind() != AlgorithmObservationModel.Kind.NONE) {
      pulse.setText(kindText(current.kind()) + "  ·  " + current.scope() + "  " + current.detail());
      // Only a forward adjacent presentation step produces a transition. Backward seek is deterministic.
      if (model.runId().equals(lastRun) && model.sequence() == lastSequence + 1) {
        pulse.setOpacity(0.4d);
        transition = new FadeTransition(Duration.millis(210), pulse);
        transition.setFromValue(0.4d);
        transition.setToValue(1.0d);
        transition.play();
      } else {
        pulse.setOpacity(1.0d);
      }
    } else {
      pulse.setOpacity(1.0d);
    }
    lastRun = model.runId();
    lastSequence = model.sequence();
    return CompletableFuture.completedFuture(null);
  }

  private static String kindText(AlgorithmObservationModel.Kind kind) {
    return I18N.text("label.algorithm.observation." + kind.name().toLowerCase(java.util.Locale.ROOT));
  }

  private static Label value() {
    Label label = new Label();
    label.setWrapText(true);
    label.setMaxWidth(Double.MAX_VALUE);
    label.getStyleClass().add("event-detail-body");
    return label;
  }

  private static void visible(javafx.scene.Node node, boolean enabled) {
    node.setManaged(enabled);
    node.setVisible(enabled);
  }
}
