package com.majortom.algorithms.visualization.render.presentation.metrics;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.metrics.MetricItem;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewModel;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewText;
import com.majortom.algorithms.visualization.render.api.PresentationRenderer;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** RenderFramework-owned FX commit for runtime statistics and run-summary presentation. */
public final class FxRuntimeOverviewRenderer
    implements PresentationRenderer<RuntimeOverviewModel> {
  private final GridPane structureMetricsGrid;
  private final VBox algorithmMetricsSection;
  private final GridPane algorithmMetricsGrid;
  private final GridPane performanceMetricsGrid;
  private final RunSummaryBindings runSummary;

  public FxRuntimeOverviewRenderer(
      GridPane structureMetricsGrid,
      VBox algorithmMetricsSection,
      GridPane algorithmMetricsGrid,
      GridPane performanceMetricsGrid,
      RunSummaryBindings runSummary) {
    this.structureMetricsGrid = Objects.requireNonNull(structureMetricsGrid, "structureMetricsGrid");
    this.algorithmMetricsSection =
        Objects.requireNonNull(algorithmMetricsSection, "algorithmMetricsSection");
    this.algorithmMetricsGrid = Objects.requireNonNull(algorithmMetricsGrid, "algorithmMetricsGrid");
    this.performanceMetricsGrid =
        Objects.requireNonNull(performanceMetricsGrid, "performanceMetricsGrid");
    this.runSummary = Objects.requireNonNull(runSummary, "runSummary");
  }

  @Override
  public CompletionStage<Void> commit(RuntimeOverviewModel model, RenderCommitContext context) {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(context, "context");
    populateMetricCards(
        structureMetricsGrid, model.structureMetrics(), "runtime-metric-card-structure");
    populateMetricCards(
        algorithmMetricsGrid, model.algorithmMetrics(), "runtime-metric-card-algorithm");
    populateMetricCards(
        performanceMetricsGrid, model.performanceMetrics(), "runtime-metric-card-performance");
    boolean hasAlgorithmMetrics = !model.algorithmMetrics().isEmpty();
    algorithmMetricsSection.setManaged(hasAlgorithmMetrics);
    algorithmMetricsSection.setVisible(hasAlgorithmMetrics);
    renderRunSummary(model);
    return CompletableFuture.completedFuture(null);
  }

  private void renderRunSummary(RuntimeOverviewModel model) {
    List<MetricItem> algorithm = model.algorithmMetrics();
    setRunMetric(runSummary.metric1Title(), runSummary.metric1Value(), algorithm, 0);
    setRunMetric(runSummary.metric2Title(), runSummary.metric2Value(), algorithm, 1);
    setRunMetric(runSummary.metric3Title(), runSummary.metric3Value(), algorithm, 2);

    MetricItem duration = model.performanceMetrics().stream()
        .filter(metric -> "totalDuration".equals(metric.key()))
        .findFirst()
        .orElse(null);
    if (duration == null) {
      runSummary.metric4Title().setText(I18N.text("label.workspace.metric.duration"));
      runSummary.metric4Value().setText("—");
    } else {
      runSummary.metric4Title().setText(RuntimeOverviewText.label(duration));
      runSummary.metric4Value().setText(RuntimeOverviewText.value(duration));
    }
  }

  private static void setRunMetric(
      Label title, Label value, List<MetricItem> metrics, int index) {
    if (index < metrics.size()) {
      MetricItem metric = metrics.get(index);
      title.setText(RuntimeOverviewText.label(metric));
      value.setText(RuntimeOverviewText.value(metric));
      return;
    }
    title.setText(I18N.text("label.workspace.metric.events"));
    value.setText("0");
  }

  private static void populateMetricCards(
      GridPane grid, List<MetricItem> metrics, String kindStyleClass) {
    boolean reusable = grid.getChildren().size() == metrics.size();
    if (reusable) {
      for (int index = 0; index < metrics.size(); index++) {
        Node node = grid.getChildren().get(index);
        if (!(node instanceof VBox card) || !metrics.get(index).key().equals(card.getUserData())) {
          reusable = false;
          break;
        }
      }
    }
    if (!reusable) {
      grid.getChildren().clear();
      for (int index = 0; index < metrics.size(); index++) {
        MetricItem metric = metrics.get(index);
        VBox card = new VBox(4.0d);
        card.setUserData(metric.key());
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().addAll("runtime-metric-card", kindStyleClass);
        if (index == 0) card.getStyleClass().add("runtime-metric-card-primary");

        Label title = new Label();
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.getStyleClass().add("runtime-metric-card-title");

        Label value = new Label();
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        value.getStyleClass().add("runtime-metric-card-value");

        card.getChildren().addAll(title, value);
        GridPane.setHgrow(card, Priority.ALWAYS);
        grid.add(card, index % 2, index / 2);
      }
    }
    for (int index = 0; index < metrics.size(); index++) {
      MetricItem metric = metrics.get(index);
      VBox card = (VBox) grid.getChildren().get(index);
      ((Label) card.getChildren().get(0)).setText(RuntimeOverviewText.label(metric));
      ((Label) card.getChildren().get(1)).setText(RuntimeOverviewText.value(metric));
    }
  }

  /** Static JavaFX targets; all dynamic run-summary values are committed by this renderer. */
  public record RunSummaryBindings(
      Label metric1Title,
      Label metric1Value,
      Label metric2Title,
      Label metric2Value,
      Label metric3Title,
      Label metric3Value,
      Label metric4Title,
      Label metric4Value) {
    public RunSummaryBindings {
      Objects.requireNonNull(metric1Title, "metric1Title");
      Objects.requireNonNull(metric1Value, "metric1Value");
      Objects.requireNonNull(metric2Title, "metric2Title");
      Objects.requireNonNull(metric2Value, "metric2Value");
      Objects.requireNonNull(metric3Title, "metric3Title");
      Objects.requireNonNull(metric3Value, "metric3Value");
      Objects.requireNonNull(metric4Title, "metric4Title");
      Objects.requireNonNull(metric4Value, "metric4Value");
    }
  }
}
