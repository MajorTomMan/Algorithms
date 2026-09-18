package com.majortom.algorithms.visualization.render.presentation.metrics;

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

/** RenderFramework-owned FX commit for the runtime statistics inspector. */
public final class FxRuntimeOverviewRenderer
    implements PresentationRenderer<RuntimeOverviewModel> {
  private final GridPane structureMetricsGrid;
  private final VBox algorithmMetricsSection;
  private final GridPane algorithmMetricsGrid;
  private final GridPane performanceMetricsGrid;

  public FxRuntimeOverviewRenderer(
      GridPane structureMetricsGrid,
      VBox algorithmMetricsSection,
      GridPane algorithmMetricsGrid,
      GridPane performanceMetricsGrid) {
    this.structureMetricsGrid = Objects.requireNonNull(structureMetricsGrid, "structureMetricsGrid");
    this.algorithmMetricsSection =
        Objects.requireNonNull(algorithmMetricsSection, "algorithmMetricsSection");
    this.algorithmMetricsGrid = Objects.requireNonNull(algorithmMetricsGrid, "algorithmMetricsGrid");
    this.performanceMetricsGrid =
        Objects.requireNonNull(performanceMetricsGrid, "performanceMetricsGrid");
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
    return CompletableFuture.completedFuture(null);
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
}
