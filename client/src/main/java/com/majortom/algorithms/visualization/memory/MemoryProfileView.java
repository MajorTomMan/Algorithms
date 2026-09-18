package com.majortom.algorithms.visualization.memory;

import com.majortom.algorithms.core.memory.MemoryCapabilities;
import com.majortom.algorithms.core.memory.MemoryAllocationSiteStat;
import com.majortom.algorithms.core.memory.MemoryAllocationTypeStat;
import com.majortom.algorithms.core.memory.MemoryDeepProfile;
import com.majortom.algorithms.core.memory.MemoryDomain;
import com.majortom.algorithms.core.memory.MemoryProfile;
import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Reusable view for one execution-scoped memory profile. */
public final class MemoryProfileView extends VBox {
    private static final MemoryCapabilities NO_CAPABILITIES =
            new MemoryCapabilities(false, false, false, false);

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
    private final MemoryAllocationChart chart = new MemoryAllocationChart();
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

    private MemoryProfile profile;
    private MemoryCapabilities capabilities = NO_CAPABILITIES;
    private MemoryDomain expectedDomain = MemoryDomain.ALGORITHM;
    private MemoryDeepProfile deepProfile;
    private StructureFootprint footprint;
    private boolean deepAnalysisSupported;
    private boolean deepAnalysisEnabled;
    private boolean footprintSupported;
    private boolean footprintBusy;
    private Consumer<Boolean> deepAnalysisToggleAction = ignored -> {};
    private Supplier<CompletionStage<StructureFootprint>> footprintAction;
    private String analysisContextId = "";

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
        I18N.localeProperty().addListener((observable, previous, current) -> refresh());
        refresh();
    }

    public void showProfile(
            MemoryProfile profile, MemoryCapabilities capabilities, MemoryDomain expectedDomain) {
        this.profile = profile;
        this.capabilities = capabilities == null ? NO_CAPABILITIES : capabilities;
        this.expectedDomain = expectedDomain == null ? MemoryDomain.ALGORITHM : expectedDomain;
        refresh();
    }


    public void showDeepAnalysis(
            String contextId,
            MemoryDeepProfile deepProfile,
            boolean deepAnalysisSupported,
            boolean deepAnalysisEnabled,
            Consumer<Boolean> deepAnalysisToggleAction,
            boolean footprintSupported,
            Supplier<CompletionStage<StructureFootprint>> footprintAction) {
        String normalizedContext = contextId == null ? "" : contextId;
        if (!normalizedContext.equals(this.analysisContextId)) {
            this.analysisContextId = normalizedContext;
            this.footprint = null;
            this.footprintBusy = false;
        }
        this.deepProfile = deepProfile;
        this.deepAnalysisSupported = deepAnalysisSupported;
        this.deepAnalysisEnabled = deepAnalysisEnabled;
        this.deepAnalysisToggleAction = deepAnalysisToggleAction == null ? ignored -> {} : deepAnalysisToggleAction;
        this.footprintSupported = footprintSupported;
        this.footprintAction = footprintAction;
        refreshDeepAnalysis();
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

        chart.setMinHeight(118.0d);
        chart.setPrefHeight(136.0d);
        chart.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(chart, Priority.NEVER);

        chartStartLabel.getStyleClass().add("memory-chart-axis-label");
        chartEndLabel.getStyleClass().add("memory-chart-axis-label");
        Region axisSpacer = new Region();
        HBox.setHgrow(axisSpacer, Priority.ALWAYS);
        HBox axis = new HBox(chartStartLabel, axisSpacer, chartEndLabel);
        axis.setAlignment(Pos.CENTER_LEFT);

        chartSection.getStyleClass().add("memory-chart-section");
        chartSection.getChildren().addAll(chartHeader, chart, axis);
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
        deepAnalysisToggle.setOnAction(event -> {
            deepAnalysisEnabled = deepAnalysisToggle.isSelected();
            deepAnalysisToggleAction.accept(deepAnalysisEnabled);
            refreshDeepAnalysis();
        });

        footprintButton.textProperty().bind(I18N.createStringBinding("action.workspace.memory.measure_footprint"));
        footprintButton.getStyleClass().addAll("button", "memory-analysis-button");
        footprintButton.setMaxWidth(Double.MAX_VALUE);
        footprintButton.setOnAction(event -> measureFootprint());

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

    private void measureFootprint() {
        Supplier<CompletionStage<StructureFootprint>> action = footprintAction;
        if (action == null || !footprintSupported) {
            return;
        }
        footprintBusy = true;
        footprintButton.setDisable(true);
        deepNotice.setText(I18N.text("label.workspace.memory.footprint_measuring"));
        deepNotice.setManaged(true);
        deepNotice.setVisible(true);
        String requestedContext = analysisContextId;
        try {
            action.get().whenComplete((result, error) -> Platform.runLater(() -> {
                if (!requestedContext.equals(analysisContextId)) {
                    return;
                }
                footprintBusy = false;
                if (error != null) {
                    footprint = StructureFootprint.unavailable(error.getMessage());
                } else {
                    footprint = result;
                }
                refreshDeepAnalysis();
            }));
        } catch (RuntimeException exception) {
            footprintBusy = false;
            footprint = StructureFootprint.unavailable(exception.getMessage());
            refreshDeepAnalysis();
        }
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

    private void refresh() {
        boolean hasProfile = profile != null;
        metricsSection.setManaged(hasProfile);
        metricsSection.setVisible(hasProfile);
        detailsSection.setManaged(hasProfile);
        detailsSection.setVisible(hasProfile);
        emptyState.setManaged(!hasProfile);
        emptyState.setVisible(!hasProfile);
        capabilityNotice.setManaged(!capabilities.threadAllocatedBytes());
        capabilityNotice.setVisible(!capabilities.threadAllocatedBytes());
        capabilityNotice.setText(I18N.text("label.workspace.memory.allocation_unavailable"));
        emptyState.setText(I18N.text(emptyKey(expectedDomain)));

        if (!hasProfile) {
            scopeLabel.setText(domainText(expectedDomain));
            statusLabel.setText(I18N.text("label.workspace.memory.waiting"));
            setStatusStyle(false);
            chartSection.setManaged(false);
            chartSection.setVisible(false);
            timingNotice.setManaged(false);
            timingNotice.setVisible(false);
            refreshDeepAnalysis();
            return;
        }

        MemoryDomain actualDomain = profile.sessionId().domain();
        scopeLabel.setText(domainText(actualDomain) + " · " + displayScope(profile.sessionId().scopeId()));
        statusLabel.setText(I18N.text(profile.complete()
                ? "label.workspace.memory.complete"
                : "label.panel.live"));
        setStatusStyle(!profile.complete());

        allocatedValue.setText(profile.allocatedBytesValue().isPresent()
                ? formatBytes(profile.allocatedBytesValue().getAsLong())
                : I18N.text("label.workspace.memory.unavailable"));
        averageRateValue.setText(profile.averageAllocationRateBytesPerSecondValue().isPresent()
                ? formatRate(profile.averageAllocationRateBytesPerSecondValue().getAsLong())
                : "—");
        peakRateValue.setText(profile.peakAllocationRateBytesPerSecondValue().isPresent()
                ? formatRate(profile.peakAllocationRateBytesPerSecondValue().getAsLong())
                : "—");
        gcValue.setText(profile.gcCollectionCountValue().isPresent()
                ? Long.toString(profile.gcCollectionCountValue().getAsLong())
                : "—");

        boolean showTimeline = profile.timingRepresentative() && profile.samples().size() >= 2;
        chartSection.setManaged(showTimeline);
        chartSection.setVisible(showTimeline);
        chart.setSamples(profile.samples());
        chartMaxLabel.setText(profile.allocatedBytesValue().isPresent()
                ? formatBytes(profile.allocatedBytesValue().getAsLong())
                : "—");
        chartEndLabel.setText(formatDuration(profile.durationNanos()));

        durationValue.setText(profile.timingRepresentative()
                ? formatDuration(profile.durationNanos())
                : "—");
        gcTimeValue.setText(profile.gcCollectionTimeMillisValue().isPresent()
                ? profile.gcCollectionTimeMillisValue().getAsLong() + " ms"
                : "—");
        heapDeltaValue.setText(profile.heapDeltaBytesValue().isPresent()
                ? formatSignedBytes(profile.heapDeltaBytesValue().getAsLong())
                : "—");
        samplesValue.setText(Integer.toString(profile.samples().size()));

        boolean timingSuppressed = !profile.timingRepresentative();
        timingNotice.setManaged(timingSuppressed);
        timingNotice.setVisible(timingSuppressed);
        timingNotice.setText(I18N.text("label.workspace.memory.timing_suppressed"));
        refreshDeepAnalysis();
    }

    private void refreshDeepAnalysis() {
        boolean structureDomain = expectedDomain == MemoryDomain.STRUCTURE;
        deepSection.setManaged(true);
        deepSection.setVisible(true);

        deepAnalysisToggle.setManaged(deepAnalysisSupported);
        deepAnalysisToggle.setVisible(deepAnalysisSupported);
        deepAnalysisToggle.setDisable(!deepAnalysisSupported);
        deepAnalysisToggle.setSelected(deepAnalysisEnabled);

        footprintButton.setManaged(structureDomain);
        footprintButton.setVisible(structureDomain);
        footprintButton.setDisable(!footprintSupported || footprintBusy);

        allocationTypes.getChildren().clear();
        allocationSites.getChildren().clear();
        boolean hasDeepSamples = deepProfile != null && deepProfile.hasSamples();
        if (hasDeepSamples) {
            allocationTypes.getChildren().add(sectionHeading("label.workspace.memory.top_allocation_types"));
            for (MemoryAllocationTypeStat stat : deepProfile.topTypes()) {
                allocationTypes.getChildren().add(analysisRow(shortClassName(stat.className()), stat.estimatedBytes()));
            }
            allocationSites.getChildren().add(sectionHeading("label.workspace.memory.top_allocation_sites"));
            for (MemoryAllocationSiteStat stat : deepProfile.topSites()) {
                allocationSites.getChildren().add(analysisRow(stat.site(), stat.estimatedBytes()));
            }
        }
        allocationTypes.setManaged(hasDeepSamples);
        allocationTypes.setVisible(hasDeepSamples);
        allocationSites.setManaged(hasDeepSamples);
        allocationSites.setVisible(hasDeepSamples);

        boolean hasFootprint = structureDomain && footprint != null && footprint.available();
        footprintBox.setManaged(hasFootprint);
        footprintBox.setVisible(hasFootprint);
        if (hasFootprint) {
            footprintBytesValue.setText(formatBytes(footprint.totalBytes()));
            footprintObjectsValue.setText(Long.toString(footprint.objectCount()));
            footprintProviderValue.setText(footprint.provider());
        }

        String noticeKey = null;
        if (!deepAnalysisSupported && !structureDomain) {
            noticeKey = "label.workspace.memory.deep_unavailable";
        } else if (deepAnalysisEnabled && deepProfile != null && !deepProfile.hasSamples()) {
            noticeKey = "label.workspace.memory.deep_no_samples";
        } else if (structureDomain && !footprintSupported) {
            noticeKey = "label.workspace.memory.footprint_unavailable";
        } else if (structureDomain && footprint != null && !footprint.available()) {
            deepNotice.setText(footprint.detail());
            deepNotice.setManaged(true);
            deepNotice.setVisible(true);
            return;
        }
        deepNotice.setManaged(noticeKey != null);
        deepNotice.setVisible(noticeKey != null);
        if (noticeKey != null) {
            deepNotice.setText(I18N.text(noticeKey));
        }
    }

    private static HBox analysisRow(String name, long bytes) {
        Label nameLabel = new Label(name);
        nameLabel.setWrapText(true);
        nameLabel.getStyleClass().add("memory-analysis-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label value = new Label(formatBytes(bytes));
        value.getStyleClass().add("memory-analysis-value");
        HBox row = new HBox(8.0d, nameLabel, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("memory-analysis-row");
        return row;
    }

    private static String shortClassName(String name) {
        if (name == null || name.isBlank()) return "unknown";
        int slash = Math.max(name.lastIndexOf('.'), name.lastIndexOf('/'));
        return slash >= 0 && slash + 1 < name.length() ? name.substring(slash + 1) : name;
    }

    private void setStatusStyle(boolean live) {
        statusPill.getStyleClass().removeAll("memory-status-live", "memory-status-complete");
        statusPill.getStyleClass().add(live ? "memory-status-live" : "memory-status-complete");
    }

    private static String emptyKey(MemoryDomain domain) {
        return switch (domain) {
            case STRUCTURE -> "label.workspace.memory.empty.structure";
            case ALGORITHM -> "label.workspace.memory.empty.algorithm";
            case PRACTICE -> "label.workspace.memory.empty.practice";
        };
    }

    private static String domainText(MemoryDomain domain) {
        return I18N.text(switch (domain) {
            case STRUCTURE -> "label.workspace.structure";
            case ALGORITHM -> "label.workspace.algorithm";
            case PRACTICE -> "label.workspace.practice";
        });
    }

    private static String displayScope(String scopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return "—";
        }
        int slash = scopeId.lastIndexOf('/');
        String value = slash >= 0 && slash + 1 < scopeId.length() ? scopeId.substring(slash + 1) : scopeId;
        return value.replace('-', ' ');
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
        if (primary) {
            card.getStyleClass().add("runtime-metric-card-primary");
        }
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

    private static String formatRate(long bytesPerSecond) {
        return formatBytes(bytesPerSecond) + "/s";
    }

    private static String formatSignedBytes(long bytes) {
        if (bytes == 0L) {
            return "0 B";
        }
        return (bytes > 0L ? "+" : "−") + formatBytes(Math.abs(bytes));
    }

    private static String formatBytes(long bytes) {
        double value = Math.max(0L, bytes);
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024.0d && unit < units.length - 1) {
            value /= 1024.0d;
            unit++;
        }
        if (unit == 0) {
            return Long.toString(bytes) + " " + units[unit];
        }
        return String.format(Locale.ROOT, value >= 100.0d ? "%.0f %s" : value >= 10.0d ? "%.1f %s" : "%.2f %s", value, units[unit]);
    }

    private static String formatDuration(long nanos) {
        if (nanos < 1_000_000L) {
            return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0d);
        }
        if (nanos < 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.0f ms", nanos / 1_000_000.0d);
        }
        return String.format(Locale.ROOT, "%.2f s", nanos / 1_000_000_000.0d);
    }
}
