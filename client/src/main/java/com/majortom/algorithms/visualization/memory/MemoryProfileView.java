package com.majortom.algorithms.visualization.memory;

import com.majortom.algorithms.visualization.international.I18N;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
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
    private final Label headerTitle = new Label();
    private final Label scopeLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox statusPill = new HBox(5.0d);
    private final VBox footprintSection = new VBox(8.0d);
    private final VBox allocationHero = new VBox(4.0d);
    private final VBox operationSummary = new VBox(8.0d);
    private final VBox rateGrid = new VBox(8.0d);
    private final VBox detailsSection = new VBox(8.0d);
    private final Label allocatedValue = valueLabel();
    private final Label allocatedTotalValue = valueLabel();
    private final Label averageRateValue = valueLabel();
    private final Label peakRateValue = valueLabel();
    private final Label durationValue = valueLabel();
    private final Label operationAllocationValue = valueLabel();
    private final Label operationDurationValue = valueLabel();
    private final Label operationAverageValue = valueLabel();
    private final Label operationPeakValue = valueLabel();
    private final Label gcValue = valueLabel();
    private final VBox chartSection = new VBox(8.0d);
    private final Pane chartHost = new Pane();
    private final Label chartMaxLabel = new Label();
    private final Label chartYMaxLabel = new Label();
    private final Label chartYMidLabel = new Label();
    private final Label chartYMinLabel = new Label();
    private final Label chartCursorLabel = new Label();
    private final Label chartStartLabel = new Label("0 ms");
    private final Label chartMiddleLabel = new Label();
    private final Label chartEndLabel = new Label();
    private final Label samplesCaption = new Label();
    private final Label samplesValue = valueLabel();
    private final Label gcTimeValue = valueLabel();
    private final Label heapDeltaValue = valueLabel();
    private final Label timingNotice = new Label();
    private final Label emptyState = new Label();
    private final Label capabilityNotice = new Label();
    private final VBox deepSection = new VBox(10.0d);
    private final CheckBox deepAnalysisToggle = new CheckBox();
    private final Button footprintButton = new Button();
    private final Label deepNotice = new Label();
    private final Label footprintNotice = new Label();
    private final VBox allocationTypes = new VBox(6.0d);
    private final VBox allocationSites = new VBox(6.0d);
    private final HBox footprintBox = new HBox(10.0d);
    private final Label footprintBytesValue = new Label("—");
    private final Label footprintObjectsValue = new Label("—");
    private final Label footprintProviderValue = new Label("—");
    private final Bindings bindings;

    private Consumer<Boolean> deepAnalysisAction = ignored -> {};
    private Runnable footprintAction = () -> {};
    private Runnable chartInvalidationAction = () -> {};

    public MemoryProfileView() {
        setSpacing(10.0d);
        setFillWidth(true);
        getStyleClass().addAll("inspector-content", "memory-profile-view");
        buildHeader();
        buildNotices();
        buildFootprintSummary();
        buildAllocationSummary();
        buildChart();
        buildDetails();
        buildDeepAnalysis();
        hideInitially(footprintSection);
        hideInitially(allocationHero);
        hideInitially(operationSummary);
        hideInitially(rateGrid);
        hideInitially(chartSection);
        hideInitially(detailsSection);
        bindings = new Bindings(
                headerTitle,
                scopeLabel,
                statusLabel,
                statusPill,
                footprintSection,
                allocationHero,
                operationSummary,
                rateGrid,
                detailsSection,
                allocatedValue,
                allocatedTotalValue,
                averageRateValue,
                peakRateValue,
                durationValue,
                operationAllocationValue,
                operationDurationValue,
                operationAverageValue,
                operationPeakValue,
                gcValue,
                chartSection,
                chartHost,
                chartMaxLabel,
                chartYMaxLabel,
                chartYMidLabel,
                chartYMinLabel,
                chartCursorLabel,
                chartStartLabel,
                chartMiddleLabel,
                chartEndLabel,
                samplesCaption,
                samplesValue,
                gcTimeValue,
                heapDeltaValue,
                timingNotice,
                emptyState,
                capabilityNotice,
                deepSection,
                deepAnalysisToggle,
                footprintButton,
                deepNotice,
                footprintNotice,
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
        VBox titleBlock = new VBox(2.0d);
        headerTitle.getStyleClass().add("runtime-overview-title");
        scopeLabel.getStyleClass().add("runtime-overview-subtitle");
        titleBlock.getChildren().addAll(headerTitle, scopeLabel);
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
        header.getStyleClass().addAll("runtime-overview-header", "memory-profile-header");
        getChildren().add(header);
    }

    private void buildNotices() {
        timingNotice.setWrapText(true);
        timingNotice.getStyleClass().add("memory-notice");
        capabilityNotice.setWrapText(true);
        capabilityNotice.getStyleClass().add("memory-notice");
        emptyState.setWrapText(true);
        emptyState.getStyleClass().add("memory-empty-state");
        hideInitially(timingNotice);
        hideInitially(capabilityNotice);
        hideInitially(emptyState);

        VBox notices = new VBox(6.0d, timingNotice, capabilityNotice, emptyState);
        notices.getStyleClass().add("memory-notice-stack");
        getChildren().add(notices);
    }

    private void buildFootprintSummary() {
        Label heading = sectionHeading("label.workspace.memory.structure_footprint");
        Label primaryTitle = new Label();
        primaryTitle.textProperty().bind(I18N.createStringBinding("label.workspace.memory.footprint_size"));
        primaryTitle.getStyleClass().add("memory-hero-title");
        footprintBytesValue.getStyleClass().add("memory-hero-value");
        footprintBytesValue.setWrapText(false);

        VBox primary = new VBox(2.0d, primaryTitle, footprintBytesValue);
        HBox primaryRow = new HBox(12.0d, primary, footprintButton);
        primaryRow.setAlignment(Pos.BOTTOM_LEFT);
        HBox.setHgrow(primary, Priority.ALWAYS);

        Label objectsTitle = inlineLabel("label.workspace.memory.footprint_objects");
        Label providerTitle = inlineLabel("label.workspace.memory.footprint_provider");
        footprintObjectsValue.getStyleClass().add("memory-inline-value");
        footprintProviderValue.getStyleClass().add("memory-inline-value");
        Region divider = new Region();
        divider.getStyleClass().add("memory-meta-divider");
        footprintBox.getStyleClass().add("memory-footprint-meta");
        footprintBox.setAlignment(Pos.CENTER_LEFT);
        footprintBox.getChildren().addAll(objectsTitle, footprintObjectsValue, divider, providerTitle, footprintProviderValue);
        HBox.setHgrow(footprintBox, Priority.ALWAYS);

        footprintButton.textProperty().bind(I18N.createStringBinding("action.workspace.memory.measure_footprint"));
        footprintButton.getStyleClass().addAll("button", "memory-analysis-button");
        footprintButton.setOnAction(event -> footprintAction.run());

        footprintNotice.setWrapText(true);
        footprintNotice.getStyleClass().add("memory-notice");
        hideInitially(footprintNotice);

        footprintSection.getStyleClass().add("memory-footprint-section");
        footprintSection.getChildren().addAll(heading, primaryRow, footprintBox, footprintNotice);
        getChildren().add(footprintSection);
    }

    private void buildAllocationSummary() {
        Label heroTitle = new Label();
        heroTitle.textProperty().bind(I18N.createStringBinding("label.workspace.memory.current_reading"));
        heroTitle.getStyleClass().add("memory-hero-title");
        allocatedValue.getStyleClass().add("memory-hero-value");
        allocatedValue.setWrapText(false);
        Label totalCaption = new Label();
        totalCaption.textProperty().bind(I18N.createStringBinding("label.workspace.memory.total_allocation"));
        totalCaption.getStyleClass().add("memory-hero-subline-title");
        allocatedTotalValue.getStyleClass().add("memory-hero-subline-value");
        HBox totalLine = new HBox(5.0d, totalCaption, allocatedTotalValue);
        totalLine.setAlignment(Pos.CENTER_LEFT);
        allocationHero.getStyleClass().add("memory-allocation-hero");
        allocationHero.getChildren().addAll(heroTitle, allocatedValue, totalLine);
        getChildren().add(allocationHero);

        Label operationHeading = sectionHeading("label.workspace.memory.metrics");
        GridPane operationGrid = metricGrid();
        addCompactMetric(operationGrid, 0, 0, "label.workspace.memory.total_allocation", operationAllocationValue);
        addCompactMetric(operationGrid, 1, 0, "label.workspace.memory.duration", operationDurationValue);
        addCompactMetric(operationGrid, 0, 1, "label.workspace.memory.average_rate", operationAverageValue);
        addCompactMetric(operationGrid, 1, 1, "label.workspace.memory.peak_rate", operationPeakValue);
        operationSummary.getStyleClass().add("memory-operation-summary");
        operationSummary.getChildren().addAll(operationHeading, operationGrid);
        getChildren().add(operationSummary);

        Label rateHeading = sectionHeading("label.workspace.memory.metrics");
        GridPane rates = metricGrid();
        addCompactMetric(rates, 0, 0, "label.workspace.memory.average_rate", averageRateValue);
        addCompactMetric(rates, 1, 0, "label.workspace.memory.peak_rate", peakRateValue);
        addCompactMetric(rates, 0, 1, "label.workspace.memory.duration", durationValue);
        rateGrid.getStyleClass().add("memory-rate-grid");
        rateGrid.getChildren().addAll(rateHeading, rates);
        getChildren().add(rateGrid);
    }

    private void buildChart() {
        Label heading = sectionHeading("label.workspace.memory.allocation_curve");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        chartMaxLabel.getStyleClass().add("memory-chart-summary");
        HBox chartHeader = new HBox(8.0d, heading, spacer, chartMaxLabel);
        chartHeader.setAlignment(Pos.CENTER_LEFT);

        chartCursorLabel.getStyleClass().add("memory-chart-readout");
        chartCursorLabel.setMaxWidth(Double.MAX_VALUE);
        chartCursorLabel.setWrapText(true);
        hideInitially(chartCursorLabel);

        chartHost.getStyleClass().add("memory-allocation-chart");
        chartHost.setMinHeight(142.0d);
        chartHost.setPrefHeight(166.0d);
        chartHost.setMinWidth(0.0d);
        chartHost.setMaxWidth(Double.MAX_VALUE);
        chartHost.widthProperty().addListener((observable, previous, current) -> chartInvalidationAction.run());
        chartHost.heightProperty().addListener((observable, previous, current) -> chartInvalidationAction.run());
        HBox.setHgrow(chartHost, Priority.ALWAYS);

        chartYMaxLabel.getStyleClass().add("memory-chart-axis-label");
        chartYMidLabel.getStyleClass().add("memory-chart-axis-label");
        chartYMinLabel.getStyleClass().add("memory-chart-axis-label");
        Region topSpacer = new Region();
        Region bottomSpacer = new Region();
        VBox yAxis = new VBox(0.0d, chartYMaxLabel, topSpacer, chartYMidLabel, bottomSpacer, chartYMinLabel);
        yAxis.getStyleClass().add("memory-chart-y-axis");
        yAxis.setAlignment(Pos.TOP_RIGHT);
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        chartStartLabel.getStyleClass().add("memory-chart-axis-label");
        chartMiddleLabel.getStyleClass().add("memory-chart-axis-label");
        chartEndLabel.getStyleClass().add("memory-chart-axis-label");
        Region axisSpacerBeforeEnd = new Region();
        Region axisSpacerAfter = new Region();
        HBox.setHgrow(axisSpacerBeforeEnd, Priority.ALWAYS);
        HBox.setHgrow(axisSpacerAfter, Priority.ALWAYS);
        HBox axis = new HBox(
                6.0d, chartStartLabel, axisSpacerBeforeEnd, chartMiddleLabel, axisSpacerAfter, chartEndLabel);
        axis.setAlignment(Pos.CENTER_LEFT);
        axis.getStyleClass().add("memory-chart-axis");
        axis.setPadding(new Insets(0.0d, 12.0d, 0.0d, 12.0d));

        GridPane chartGrid = new GridPane();
        chartGrid.setHgap(6.0d);
        chartGrid.setVgap(2.0d);
        chartGrid.setMaxWidth(Double.MAX_VALUE);
        chartGrid.getStyleClass().add("memory-chart-plot");
        ColumnConstraints yAxisColumn = new ColumnConstraints();
        ColumnConstraints plotColumn = new ColumnConstraints();
        plotColumn.setHgrow(Priority.ALWAYS);
        plotColumn.setFillWidth(true);
        chartGrid.getColumnConstraints().addAll(yAxisColumn, plotColumn);
        chartGrid.add(yAxis, 0, 0);
        chartGrid.add(chartHost, 1, 0);
        chartGrid.add(axis, 1, 1);
        GridPane.setHgrow(chartHost, Priority.ALWAYS);
        GridPane.setHgrow(axis, Priority.ALWAYS);

        samplesCaption.textProperty().bind(I18N.createStringBinding("label.workspace.memory.samples"));
        samplesCaption.getStyleClass().add("memory-chart-axis-label");
        samplesValue.getStyleClass().add("memory-chart-samples");
        HBox footer = new HBox(6.0d, samplesCaption, samplesValue);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("memory-chart-footer");

        chartSection.getStyleClass().add("memory-chart-section");
        chartSection.getChildren().addAll(chartHeader, chartCursorLabel, chartGrid, footer);
        getChildren().add(chartSection);
    }

    private void buildDetails() {
        Label note = new Label();
        note.textProperty().bind(I18N.createStringBinding("label.workspace.memory.jvm_reference_note"));
        note.setWrapText(true);
        note.getStyleClass().add("memory-reference-note");

        GridPane grid = new GridPane();
        grid.setHgap(7.0d);
        grid.setVgap(7.0d);
        grid.setMaxWidth(Double.MAX_VALUE);
        for (int index = 0; index < 2; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50.0d);
            grid.getColumnConstraints().add(column);
        }
        addDetailTile(grid, 0, 0, "label.workspace.memory.heap_delta", heapDeltaValue);
        addDetailTile(grid, 1, 0, "label.workspace.memory.gc", gcValue);
        addDetailTile(grid, 0, 1, "label.workspace.memory.gc_time", gcTimeValue);
        detailsSection.getStyleClass().add("memory-details-section");
        detailsSection.getChildren().addAll(note, grid);
        getChildren().add(disclosure("label.workspace.memory.jvm_reference", detailsSection));
    }

    private void buildDeepAnalysis() {
        deepAnalysisToggle.textProperty().bind(I18N.createStringBinding("label.workspace.memory.deep_toggle"));
        deepAnalysisToggle.setWrapText(true);
        deepAnalysisToggle.setMaxWidth(Double.MAX_VALUE);
        deepAnalysisToggle.setOnAction(event -> deepAnalysisAction.accept(deepAnalysisToggle.isSelected()));

        deepNotice.setWrapText(true);
        deepNotice.getStyleClass().add("memory-notice");
        hideInitially(deepNotice);
        allocationTypes.getStyleClass().add("memory-analysis-list");
        allocationSites.getStyleClass().add("memory-analysis-list");
        deepSection.getStyleClass().add("memory-deep-section");
        deepSection.getChildren().addAll(deepAnalysisToggle, deepNotice, allocationTypes, allocationSites);
        getChildren().add(disclosure("label.workspace.memory.jfr_analysis", deepSection));
    }

    private static TitledPane disclosure(String titleKey, Node content) {
        TitledPane disclosure = new TitledPane();
        disclosure.textProperty().bind(I18N.createStringBinding(titleKey));
        disclosure.setContent(content);
        disclosure.setExpanded(false);
        disclosure.setAnimated(false);
        disclosure.setMinWidth(0.0d);
        disclosure.setPrefWidth(0.0d);
        disclosure.setMaxWidth(Double.MAX_VALUE);
        disclosure.getStyleClass().add("memory-disclosure");
        return disclosure;
    }

    private static GridPane metricGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(7.0d);
        grid.setVgap(5.0d);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getStyleClass().add("memory-compact-metric-grid");
        for (int index = 0; index < 2; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50.0d);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private static void addCompactMetric(GridPane grid, int column, int row, String key, Label value) {
        VBox cell = new VBox(2.0d);
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.getStyleClass().add("memory-mini-metric-card");
        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding(key));
        title.setWrapText(true);
        title.getStyleClass().add("memory-mini-metric-title");
        value.getStyleClass().add("memory-mini-metric-value");
        value.setWrapText(false);
        cell.getChildren().addAll(title, value);
        GridPane.setHgrow(cell, Priority.ALWAYS);
        grid.add(cell, column, row);
    }

    private static Label inlineLabel(String key) {
        Label label = new Label();
        label.textProperty().bind(I18N.createStringBinding(key));
        label.getStyleClass().add("memory-inline-label");
        return label;
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

    private static void addDetailTile(GridPane grid, int column, int row, String key, Label value) {
        VBox tile = new VBox(2.0d);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.getStyleClass().add("memory-detail-tile");
        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding(key));
        title.setWrapText(true);
        title.getStyleClass().add("memory-detail-label");
        value.getStyleClass().add("memory-detail-value");
        value.setMaxWidth(Double.MAX_VALUE);
        tile.getChildren().addAll(title, value);
        GridPane.setHgrow(tile, Priority.ALWAYS);
        grid.add(tile, column, row);
    }

    private static void hideInitially(Node node) {
        node.setManaged(false);
        node.setVisible(false);
    }

    public record Bindings(
            Label headerTitle,
            Label scopeLabel,
            Label statusLabel,
            HBox statusPill,
            VBox footprintSection,
            VBox allocationHero,
            VBox operationSummary,
            VBox rateGrid,
            VBox detailsSection,
            Label allocatedValue,
            Label allocatedTotalValue,
            Label averageRateValue,
            Label peakRateValue,
            Label durationValue,
            Label operationAllocationValue,
            Label operationDurationValue,
            Label operationAverageValue,
            Label operationPeakValue,
            Label gcValue,
            VBox chartSection,
            Pane chartHost,
            Label chartMaxLabel,
            Label chartYMaxLabel,
            Label chartYMidLabel,
            Label chartYMinLabel,
            Label chartCursorLabel,
            Label chartStartLabel,
            Label chartMiddleLabel,
            Label chartEndLabel,
            Label samplesCaption,
            Label samplesValue,
            Label gcTimeValue,
            Label heapDeltaValue,
            Label timingNotice,
            Label emptyState,
            Label capabilityNotice,
            VBox deepSection,
            CheckBox deepAnalysisToggle,
            Button footprintButton,
            Label deepNotice,
            Label footprintNotice,
            VBox allocationTypes,
            VBox allocationSites,
            HBox footprintBox,
            Label footprintBytesValue,
            Label footprintObjectsValue,
            Label footprintProviderValue) {
        public Bindings {
            Objects.requireNonNull(headerTitle, "headerTitle");
            Objects.requireNonNull(scopeLabel, "scopeLabel");
            Objects.requireNonNull(statusLabel, "statusLabel");
            Objects.requireNonNull(statusPill, "statusPill");
            Objects.requireNonNull(chartHost, "chartHost");
        }
    }
}
