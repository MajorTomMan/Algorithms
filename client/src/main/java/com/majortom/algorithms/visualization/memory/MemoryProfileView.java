package com.majortom.algorithms.visualization.memory;

import com.majortom.algorithms.visualization.international.I18N;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Passive JavaFX host for the Memory inspector.
 *
 * <p>This class owns static UI composition and user-intent callbacks only. It does not read telemetry,
 * project playback time, format runtime values, or commit dynamic presentation state. Those
 * responsibilities belong to the RenderFramework-owned {@code FxMemoryRenderer}.</p>
 */
public final class MemoryProfileView extends VBox {
    private final Label scopeLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox statusPill = new HBox(5.0d);
    private final Label allocatedValue = valueLabel();
    private final Label averageRateValue = valueLabel();
    private final Label peakRateValue = valueLabel();
    private final Label gcValue = valueLabel();
    private final VBox metricsSection = new VBox(8.0d);
    private final VBox chartSection = new VBox(8.0d);
    private final VBox detailsSection = new VBox(8.0d);
    private final Pane chartHost = new Pane();
    private final Label chartMaxLabel = new Label();
    private final Label chartStartLabel = new Label("0 ms");
    private final Label chartEndLabel = new Label();
    private final Label durationValue = new Label();
    private final Label gcTimeValue = new Label();
    private final Label heapDeltaValue = new Label();
    private final Label samplesValue = new Label();
    private final Label timingNotice = new Label();
    private final Label emptyState = new Label();
    private final Label capabilityNotice = new Label();
    private final VBox deepSection = new VBox(10.0d);
    private final CheckBox deepAnalysisToggle = new CheckBox();
    private final Button footprintButton = new Button();
    private final Label deepNotice = new Label();
    private final VBox allocationTypes = new VBox(6.0d);
    private final VBox allocationSites = new VBox(6.0d);
    private final VBox footprintBox = new VBox(7.0d);
    private final Label footprintBytesValue = new Label("—");
    private final Label footprintObjectsValue = new Label("—");
    private final Label footprintProviderValue = new Label("—");
    private final Bindings bindings;

    private Consumer<Boolean> deepAnalysisAction = ignored -> {};
    private Runnable footprintAction = () -> {};
    private Runnable chartInvalidationAction = () -> {};

    public MemoryProfileView() {
        setSpacing(14.0d);
        setFillWidth(true);
        getStyleClass().addAll("inspector-content", "memory-profile-view");
        buildHeader();
        buildMetrics();
        buildChart();
        buildDetails();
        buildDeepAnalysis();
        buildNotices();
        bindings = new Bindings(
                scopeLabel,
                statusLabel,
                statusPill,
                allocatedValue,
                averageRateValue,
                peakRateValue,
                gcValue,
                metricsSection,
                chartSection,
                detailsSection,
                chartHost,
                chartMaxLabel,
                chartStartLabel,
                chartEndLabel,
                durationValue,
                gcTimeValue,
                heapDeltaValue,
                samplesValue,
                timingNotice,
                emptyState,
                capabilityNotice,
                deepSection,
                deepAnalysisToggle,
                footprintButton,
                deepNotice,
                allocationTypes,
                allocationSites,
                footprintBox,
                footprintBytesValue,
                footprintObjectsValue,
                footprintProviderValue);
    }

    /** JavaFX targets used only by the RenderFramework-owned Memory renderer. */
    public Bindings bindings() {
        return bindings;
    }

    /** User intent only; this does not commit presentation state. */
    public void setDeepAnalysisAction(Consumer<Boolean> action) {
        deepAnalysisAction = action == null ? ignored -> {} : action;
    }

    /** User intent only; the analysis result is later published back through the presentation source. */
    public void setFootprintAction(Runnable action) {
        footprintAction = action == null ? () -> {} : action;
    }

    /** UI geometry invalidation is routed back into RenderFramework instead of redrawing locally. */
    public void setChartInvalidationAction(Runnable action) {
        chartInvalidationAction = action == null ? () -> {} : action;
    }

    private void buildHeader() {
        VBox titleBlock = new VBox(3.0d);
        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding("label.workspace.memory.overview"));
        title.getStyleClass().add("runtime-overview-title");
        scopeLabel.getStyleClass().add("runtime-overview-subtitle");
        titleBlock.getChildren().addAll(title, scopeLabel);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Region dot = new Region();
        dot.getStyleClass().add("memory-status-dot");
        statusLabel.getStyleClass().add("memory-status-label");
        statusPill.setAlignment(Pos.CENTER);
        statusPill.setMaxHeight(Region.USE_PREF_SIZE);
        statusPill.getStyleClass().add("memory-status-pill");
        statusPill.getChildren().addAll(dot, statusLabel);

        HBox header = new HBox(10.0d, titleBlock, statusPill);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFillHeight(false);
        header.getStyleClass().add("runtime-overview-header");
        getChildren().add(header);
    }

    private void buildMetrics() {
        Label heading = sectionHeading("label.workspace.memory.metrics");
        GridPane grid = metricGrid();
        addMetricCard(grid, 0, 0, "label.workspace.memory.allocated", allocatedValue, true);
        addMetricCard(grid, 1, 0, "label.workspace.memory.average_rate", averageRateValue, false);
        addMetricCard(grid, 0, 1, "label.workspace.memory.peak_rate", peakRateValue, false);
        addMetricCard(grid, 1, 1, "label.workspace.memory.gc", gcValue, false);
        metricsSection.getStyleClass().add("runtime-metrics-section");
        metricsSection.getChildren().addAll(heading, grid);
        getChildren().add(metricsSection);
    }

    private void buildChart() {
        Label heading = sectionHeading("label.workspace.memory.allocation_curve");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        chartMaxLabel.getStyleClass().add("memory-chart-summary");
        HBox chartHeader = new HBox(8.0d, heading, spacer, chartMaxLabel);
        chartHeader.setAlignment(Pos.CENTER_LEFT);

        chartHost.getStyleClass().add("memory-allocation-chart");
        chartHost.setMinHeight(118.0d);
        chartHost.setPrefHeight(136.0d);
        chartHost.setMaxWidth(Double.MAX_VALUE);
        chartHost.widthProperty().addListener((observable, previous, current) -> chartInvalidationAction.run());
        chartHost.heightProperty().addListener((observable, previous, current) -> chartInvalidationAction.run());
        VBox.setVgrow(chartHost, Priority.NEVER);

        chartStartLabel.getStyleClass().add("memory-chart-axis-label");
        chartEndLabel.getStyleClass().add("memory-chart-axis-label");
        Region axisSpacer = new Region();
        HBox.setHgrow(axisSpacer, Priority.ALWAYS);
        HBox axis = new HBox(chartStartLabel, axisSpacer, chartEndLabel);
        axis.setAlignment(Pos.CENTER_LEFT);

        chartSection.getStyleClass().add("memory-chart-section");
        chartSection.getChildren().addAll(chartHeader, chartHost, axis);
        getChildren().add(chartSection);
    }

    private void buildDetails() {
        detailsSection.getStyleClass().add("memory-details-section");
        detailsSection.getChildren().add(sectionHeading("label.workspace.memory.details"));

        GridPane grid = new GridPane();
        grid.setHgap(12.0d);
        grid.setVgap(7.0d);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPercentWidth(54.0d);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setPercentWidth(46.0d);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);
        addDetailRow(grid, 0, "label.workspace.memory.duration", durationValue);
        addDetailRow(grid, 1, "label.workspace.memory.gc_time", gcTimeValue);
        addDetailRow(grid, 2, "label.workspace.memory.heap_delta", heapDeltaValue);
        addDetailRow(grid, 3, "label.workspace.memory.samples", samplesValue);
        detailsSection.getChildren().add(grid);
        getChildren().add(detailsSection);
    }

    private void buildDeepAnalysis() {
        Label heading = sectionHeading("label.workspace.memory.deep_analysis");
        deepAnalysisToggle.textProperty().bind(I18N.createStringBinding("label.workspace.memory.deep_toggle"));
        deepAnalysisToggle.setOnAction(event -> deepAnalysisAction.accept(deepAnalysisToggle.isSelected()));

        footprintButton.textProperty().bind(I18N.createStringBinding("action.workspace.memory.measure_footprint"));
        footprintButton.getStyleClass().addAll("button", "memory-analysis-button");
        footprintButton.setMaxWidth(Double.MAX_VALUE);
        footprintButton.setOnAction(event -> footprintAction.run());

        deepNotice.setWrapText(true);
        deepNotice.getStyleClass().add("memory-notice");
        allocationTypes.getStyleClass().add("memory-analysis-list");
        allocationSites.getStyleClass().add("memory-analysis-list");
        footprintBox.getStyleClass().add("memory-footprint-box");

        GridPane footprintGrid = new GridPane();
        footprintGrid.setHgap(12.0d);
        footprintGrid.setVgap(6.0d);
        addDetailRow(footprintGrid, 0, "label.workspace.memory.footprint_size", footprintBytesValue);
        addDetailRow(footprintGrid, 1, "label.workspace.memory.footprint_objects", footprintObjectsValue);
        addDetailRow(footprintGrid, 2, "label.workspace.memory.footprint_provider", footprintProviderValue);
        footprintBox.getChildren().addAll(sectionHeading("label.workspace.memory.structure_footprint"), footprintGrid);

        deepSection.getStyleClass().add("memory-deep-section");
        deepSection.getChildren().addAll(
                heading, deepAnalysisToggle, footprintButton, deepNotice, allocationTypes, allocationSites, footprintBox);
        getChildren().add(deepSection);
    }

    private void buildNotices() {
        timingNotice.setWrapText(true);
        timingNotice.getStyleClass().add("memory-notice");
        capabilityNotice.setWrapText(true);
        capabilityNotice.getStyleClass().add("memory-notice");
        emptyState.setWrapText(true);
        emptyState.getStyleClass().add("memory-empty-state");
        getChildren().addAll(timingNotice, capabilityNotice, emptyState);
    }

    private static GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8.0d);
        grid.setVgap(8.0d);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getStyleClass().add("runtime-metric-grid");
        ColumnConstraints first = new ColumnConstraints();
        first.setPercentWidth(50.0d);
        ColumnConstraints second = new ColumnConstraints();
        second.setPercentWidth(50.0d);
        grid.getColumnConstraints().addAll(first, second);
        return grid;
    }

    private static void addMetricCard(
            GridPane grid, int column, int row, String labelKey, Label value, boolean primary) {
        VBox card = new VBox(4.0d);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().addAll("runtime-metric-card", "memory-metric-card");
        if (primary) card.getStyleClass().add("runtime-metric-card-primary");
        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding(labelKey));
        title.setWrapText(true);
        title.getStyleClass().add("runtime-metric-card-title");
        value.getStyleClass().add("runtime-metric-card-value");
        value.setWrapText(true);
        card.getChildren().addAll(title, value);
        GridPane.setHgrow(card, Priority.ALWAYS);
        grid.add(card, column, row);
    }

    private static Label valueLabel() {
        Label label = new Label("—");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Label sectionHeading(String key) {
        Label label = new Label();
        label.textProperty().bind(I18N.createStringBinding(key));
        label.getStyleClass().add("runtime-section-title");
        return label;
    }

    private static void addDetailRow(GridPane grid, int row, String key, Label value) {
        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding(key));
        title.getStyleClass().add("memory-detail-label");
        value.getStyleClass().add("memory-detail-value");
        value.setMaxWidth(Double.MAX_VALUE);
        grid.add(title, 0, row);
        grid.add(value, 1, row);
    }

    public record Bindings(
            Label scopeLabel,
            Label statusLabel,
            HBox statusPill,
            Label allocatedValue,
            Label averageRateValue,
            Label peakRateValue,
            Label gcValue,
            VBox metricsSection,
            VBox chartSection,
            VBox detailsSection,
            Pane chartHost,
            Label chartMaxLabel,
            Label chartStartLabel,
            Label chartEndLabel,
            Label durationValue,
            Label gcTimeValue,
            Label heapDeltaValue,
            Label samplesValue,
            Label timingNotice,
            Label emptyState,
            Label capabilityNotice,
            VBox deepSection,
            CheckBox deepAnalysisToggle,
            Button footprintButton,
            Label deepNotice,
            VBox allocationTypes,
            VBox allocationSites,
            VBox footprintBox,
            Label footprintBytesValue,
            Label footprintObjectsValue,
            Label footprintProviderValue) {
        public Bindings {
            Objects.requireNonNull(scopeLabel, "scopeLabel");
            Objects.requireNonNull(statusLabel, "statusLabel");
            Objects.requireNonNull(statusPill, "statusPill");
            Objects.requireNonNull(chartHost, "chartHost");
        }
    }
}
