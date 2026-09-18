package com.majortom.algorithms.visualization.render.presentation.memory;

import com.majortom.algorithms.telemetry.api.TelemetryDomain;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationAnalysis;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationSiteStat;
import com.majortom.algorithms.telemetry.memory.analysis.MemoryAllocationTypeStat;
import com.majortom.algorithms.telemetry.memory.analysis.StructureFootprint;
import com.majortom.algorithms.telemetry.memory.api.MemoryAllocationSample;
import com.majortom.algorithms.telemetry.memory.api.MemoryFacts;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.memory.MemoryProfileView;
import com.majortom.algorithms.visualization.render.api.PresentationRenderer;
import com.majortom.algorithms.visualization.render.api.RenderCommitContext;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

/** RenderFramework-owned JavaFX commit for execution-scoped Memory presentation. */
public final class FxMemoryRenderer implements PresentationRenderer<MemoryPresentationModel> {
  private static final double LEFT_INSET = 10.0d;
  private static final double RIGHT_INSET = 10.0d;
  private static final double TOP_INSET = 10.0d;
  private static final double BOTTOM_INSET = 10.0d;

  private final MemoryProfileView.Bindings view;
  private final Line topGrid = gridLine();
  private final Line middleGrid = gridLine();
  private final Line bottomGrid = gridLine();
  private final Path curve = new Path();
  private final Circle cursorMarker = new Circle(3.5d);
  private final Rectangle clip = new Rectangle();
  private boolean chartInstalled;

  public FxMemoryRenderer(MemoryProfileView target) {
    this.view = Objects.requireNonNull(target, "target").bindings();
    curve.getStyleClass().add("memory-allocation-curve");
    curve.setMouseTransparent(true);
    cursorMarker.getStyleClass().add("memory-allocation-cursor");
    cursorMarker.setMouseTransparent(true);
  }

  @Override
  public CompletionStage<Void> commit(MemoryPresentationModel model, RenderCommitContext context) {
    Objects.requireNonNull(model, "model");
    Objects.requireNonNull(context, "context");
    ensureChartInstalled();
    renderProfile(model);
    renderDeepAnalysis(model);
    renderChart(model);
    return CompletableFuture.completedFuture(null);
  }

  private void renderProfile(MemoryPresentationModel model) {
    MemoryFacts profile = model.facts().orElse(null);
    boolean hasProfile = profile != null;
    visible(view.metricsSection(), hasProfile);
    visible(view.detailsSection(), hasProfile);
    visible(view.emptyState(), !hasProfile);

    boolean allocationUnavailable = !model.capabilities().threadAllocatedBytes();
    visible(view.capabilityNotice(), allocationUnavailable);
    view.capabilityNotice().setText(I18N.text("label.workspace.memory.allocation_unavailable"));
    view.emptyState().setText(I18N.text(emptyKey(model.expectedDomain())));

    if (!hasProfile) {
      view.scopeLabel().setText(domainText(model.expectedDomain()));
      view.statusLabel().setText(I18N.text("label.workspace.memory.waiting"));
      setStatusStyle(false);
      visible(view.chartSection(), false);
      visible(view.timingNotice(), false);
      resetMetricValues();
      return;
    }

    TelemetryDomain actualDomain = profile.sessionId().scope().domain();
    view.scopeLabel().setText(
        domainText(actualDomain) + " · " + displayScope(profile.sessionId().scope().stableId()));
    view.statusLabel().setText(I18N.text(
        profile.complete() ? "label.workspace.memory.complete" : "label.panel.live"));
    setStatusStyle(!profile.complete());

    OptionalLong displayedAllocation = model.cursorProjected()
        ? model.currentAllocatedBytes()
        : profile.allocatedBytesValue();
    view.allocatedValue().setText(displayedAllocation.isPresent()
        ? formatBytes(displayedAllocation.getAsLong())
        : I18N.text("label.workspace.memory.unavailable"));
    view.averageRateValue().setText(profile.averageAllocationRateBytesPerSecondValue().isPresent()
        ? formatRate(profile.averageAllocationRateBytesPerSecondValue().getAsLong())
        : "—");
    view.peakRateValue().setText(profile.peakAllocationRateBytesPerSecondValue().isPresent()
        ? formatRate(profile.peakAllocationRateBytesPerSecondValue().getAsLong())
        : "—");
    view.gcValue().setText(profile.gcCollectionCountValue().isPresent()
        ? Long.toString(profile.gcCollectionCountValue().getAsLong())
        : "—");

    boolean showTimeline = profile.timingRepresentative() && model.timelineSamples().size() >= 2;
    visible(view.chartSection(), showTimeline);
    OptionalLong chartMaximum = chartMaximum(profile, model.timelineSamples());
    view.chartMaxLabel().setText(chartMaximum.isPresent()
        ? formatBytes(chartMaximum.getAsLong())
        : "—");
    view.chartStartLabel().setText("0 ms");
    view.chartEndLabel().setText(formatDuration(profile.durationNanos()));

    long displayedDuration = model.cursorProjected()
        ? model.visibleElapsedNanos()
        : profile.durationNanos();
    view.durationValue().setText(profile.timingRepresentative()
        ? formatDuration(displayedDuration)
        : "—");
    view.gcTimeValue().setText(profile.gcCollectionTimeMillisValue().isPresent()
        ? profile.gcCollectionTimeMillisValue().getAsLong() + " ms"
        : "—");
    view.heapDeltaValue().setText(profile.heapDeltaBytesValue().isPresent()
        ? formatSignedBytes(profile.heapDeltaBytesValue().getAsLong())
        : "—");
    view.samplesValue().setText(Integer.toString(
        model.cursorProjected()
            ? visibleSampleCount(model.timelineSamples(), model.visibleElapsedNanos())
            : model.timelineSamples().size()));

    boolean timingSuppressed = !profile.timingRepresentative();
    visible(view.timingNotice(), timingSuppressed);
    view.timingNotice().setText(I18N.text("label.workspace.memory.timing_suppressed"));
  }

  private void renderDeepAnalysis(MemoryPresentationModel model) {
    boolean structureDomain = model.expectedDomain() == TelemetryDomain.STRUCTURE;
    visible(view.deepSection(), true);

    boolean deepSupported = model.capabilities().jfrAvailable();
    visible(view.deepAnalysisToggle(), deepSupported);
    view.deepAnalysisToggle().setDisable(!deepSupported);
    view.deepAnalysisToggle().setSelected(model.deepAnalysisEnabled());

    visible(view.footprintButton(), structureDomain);
    view.footprintButton().setDisable(!model.footprintSupported() || model.footprintBusy());

    view.allocationTypes().getChildren().clear();
    view.allocationSites().getChildren().clear();
    MemoryAllocationAnalysis deep = model.deepAnalysis().orElse(null);
    boolean hasDeepSamples = deep != null && deep.hasSamples();
    if (hasDeepSamples) {
      view.allocationTypes().getChildren().add(sectionHeading("label.workspace.memory.top_allocation_types"));
      for (MemoryAllocationTypeStat stat : deep.topTypes()) {
        view.allocationTypes().getChildren().add(
            analysisRow(shortClassName(stat.className()), stat.estimatedBytes()));
      }
      view.allocationSites().getChildren().add(sectionHeading("label.workspace.memory.top_allocation_sites"));
      for (MemoryAllocationSiteStat stat : deep.topSites()) {
        view.allocationSites().getChildren().add(analysisRow(stat.site(), stat.estimatedBytes()));
      }
    }
    visible(view.allocationTypes(), hasDeepSamples);
    visible(view.allocationSites(), hasDeepSamples);

    StructureFootprint footprint = model.footprint().orElse(null);
    boolean hasFootprint = structureDomain && footprint != null && footprint.available();
    visible(view.footprintBox(), hasFootprint);
    if (hasFootprint) {
      view.footprintBytesValue().setText(formatBytes(footprint.totalBytes()));
      view.footprintObjectsValue().setText(Long.toString(footprint.objectCount()));
      view.footprintProviderValue().setText(footprint.provider());
    }

    if (model.footprintBusy() && structureDomain) {
      showDeepNotice(I18N.text("label.workspace.memory.footprint_measuring"));
      return;
    }
    if (!deepSupported && !structureDomain) {
      showDeepNotice(I18N.text("label.workspace.memory.deep_unavailable"));
      return;
    }
    if (model.deepAnalysisEnabled() && deep != null && !deep.hasSamples()) {
      showDeepNotice(I18N.text("label.workspace.memory.deep_no_samples"));
      return;
    }
    if (structureDomain && !model.footprintSupported()) {
      showDeepNotice(I18N.text("label.workspace.memory.footprint_unavailable"));
      return;
    }
    if (structureDomain && footprint != null && !footprint.available()) {
      showDeepNotice(footprint.detail());
      return;
    }
    visible(view.deepNotice(), false);
  }

  private void renderChart(MemoryPresentationModel model) {
    Pane host = view.chartHost();
    double width = Math.max(0.0d, host.getWidth());
    double height = Math.max(0.0d, host.getHeight());
    clip.setWidth(width);
    clip.setHeight(height);

    double x0 = LEFT_INSET;
    double x1 = Math.max(x0, width - RIGHT_INSET);
    double y0 = TOP_INSET;
    double y1 = Math.max(y0, height - BOTTOM_INSET);
    double middle = y0 + (y1 - y0) * 0.5d;
    positionGrid(topGrid, x0, x1, y0);
    positionGrid(middleGrid, x0, x1, middle);
    positionGrid(bottomGrid, x0, x1, y1);

    curve.getElements().clear();
    cursorMarker.setVisible(false);
    List<MemoryAllocationSample> samples = model.timelineSamples();
    MemoryFacts profile = model.facts().orElse(null);
    if (profile == null || samples.isEmpty() || x1 <= x0 || y1 <= y0) return;

    long maxElapsed = Math.max(1L, Math.max(profile.durationNanos(), samples.getLast().elapsedNanos()));
    long maxAllocated = profile.allocatedBytesValue().orElse(0L);
    for (MemoryAllocationSample sample : samples) {
      maxAllocated = Math.max(maxAllocated, sample.allocatedBytes());
    }
    maxAllocated = Math.max(1L, maxAllocated);

    long visibleElapsed = model.cursorProjected()
        ? Math.min(maxElapsed, model.visibleElapsedNanos())
        : maxElapsed;
    boolean started = false;
    for (MemoryAllocationSample sample : samples) {
      if (sample.elapsedNanos() > visibleElapsed) break;
      double x = chartX(sample.elapsedNanos(), maxElapsed, x0, x1);
      double y = chartY(sample.allocatedBytes(), maxAllocated, y0, y1);
      if (!started) {
        curve.getElements().add(new MoveTo(x, y));
        started = true;
      } else {
        curve.getElements().add(new LineTo(x, y));
      }
    }

    OptionalLong currentBytes = model.currentAllocatedBytes();
    if (currentBytes.isPresent() && visibleElapsed >= 0L) {
      double x = chartX(visibleElapsed, maxElapsed, x0, x1);
      double y = chartY(currentBytes.getAsLong(), maxAllocated, y0, y1);
      if (!started) {
        curve.getElements().add(new MoveTo(x0, y1));
        started = true;
      }
      MemoryAllocationSample lastVisible = lastVisibleSample(samples, visibleElapsed);
      if (lastVisible == null
          || lastVisible.elapsedNanos() != visibleElapsed
          || lastVisible.allocatedBytes() != currentBytes.getAsLong()) {
        curve.getElements().add(new LineTo(x, y));
      }
      cursorMarker.setCenterX(x);
      cursorMarker.setCenterY(y);
      cursorMarker.setVisible(true);
    }
  }

  private void ensureChartInstalled() {
    if (chartInstalled) return;
    view.chartHost().getChildren().setAll(topGrid, middleGrid, bottomGrid, curve, cursorMarker);
    view.chartHost().setClip(clip);
    chartInstalled = true;
  }

  private void resetMetricValues() {
    view.allocatedValue().setText("—");
    view.averageRateValue().setText("—");
    view.peakRateValue().setText("—");
    view.gcValue().setText("—");
    view.chartMaxLabel().setText("—");
    view.chartEndLabel().setText("—");
    view.durationValue().setText("—");
    view.gcTimeValue().setText("—");
    view.heapDeltaValue().setText("—");
    view.samplesValue().setText("0");
  }

  private void showDeepNotice(String text) {
    view.deepNotice().setText(text == null || text.isBlank() ? "—" : text);
    visible(view.deepNotice(), true);
  }

  private void setStatusStyle(boolean live) {
    view.statusPill().getStyleClass().removeAll("memory-status-live", "memory-status-complete");
    view.statusPill().getStyleClass().add(live ? "memory-status-live" : "memory-status-complete");
  }

  private static void visible(javafx.scene.Node node, boolean visible) {
    node.setManaged(visible);
    node.setVisible(visible);
  }


  private static OptionalLong chartMaximum(MemoryFacts profile, List<MemoryAllocationSample> samples) {
    long maximum = 0L;
    boolean measured = false;
    OptionalLong total = profile.allocatedBytesValue();
    if (total.isPresent()) {
      maximum = Math.max(maximum, total.getAsLong());
      measured = true;
    }
    for (MemoryAllocationSample sample : samples) {
      maximum = Math.max(maximum, sample.allocatedBytes());
      measured = true;
    }
    return measured ? OptionalLong.of(maximum) : OptionalLong.empty();
  }

  private static int visibleSampleCount(List<MemoryAllocationSample> samples, long elapsedNanos) {
    int count = 0;
    for (MemoryAllocationSample sample : samples) {
      if (sample.elapsedNanos() > elapsedNanos) break;
      count++;
    }
    return count;
  }

  private static MemoryAllocationSample lastVisibleSample(
      List<MemoryAllocationSample> samples, long elapsedNanos) {
    MemoryAllocationSample result = null;
    for (MemoryAllocationSample sample : samples) {
      if (sample.elapsedNanos() > elapsedNanos) break;
      result = sample;
    }
    return result;
  }

  private static double chartX(long elapsed, long maxElapsed, double x0, double x1) {
    return x0 + ((double) Math.max(0L, elapsed) / Math.max(1L, maxElapsed)) * (x1 - x0);
  }

  private static double chartY(long allocated, long maxAllocated, double y0, double y1) {
    return y1 - ((double) Math.max(0L, allocated) / Math.max(1L, maxAllocated)) * (y1 - y0);
  }

  private static Line gridLine() {
    Line line = new Line();
    line.getStyleClass().add("memory-chart-grid-line");
    line.setMouseTransparent(true);
    return line;
  }

  private static void positionGrid(Line line, double x0, double x1, double y) {
    line.setStartX(x0);
    line.setEndX(x1);
    line.setStartY(y);
    line.setEndY(y);
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

  private static Label sectionHeading(String key) {
    Label label = new Label(I18N.text(key));
    label.getStyleClass().add("runtime-section-title");
    return label;
  }

  private static String shortClassName(String name) {
    if (name == null || name.isBlank()) return "unknown";
    int slash = Math.max(name.lastIndexOf('.'), name.lastIndexOf('/'));
    return slash >= 0 && slash + 1 < name.length() ? name.substring(slash + 1) : name;
  }

  private static String emptyKey(TelemetryDomain domain) {
    return switch (domain) {
      case STRUCTURE -> "label.workspace.memory.empty.structure";
      case ALGORITHM -> "label.workspace.memory.empty.algorithm";
      case PRACTICE -> "label.workspace.memory.empty.practice";
    };
  }

  private static String domainText(TelemetryDomain domain) {
    return I18N.text(switch (domain) {
      case STRUCTURE -> "label.workspace.structure";
      case ALGORITHM -> "label.workspace.algorithm";
      case PRACTICE -> "label.workspace.practice";
    });
  }

  private static String displayScope(String scopeId) {
    if (scopeId == null || scopeId.isBlank()) return "—";
    int slash = scopeId.lastIndexOf('/');
    String value = slash >= 0 && slash + 1 < scopeId.length() ? scopeId.substring(slash + 1) : scopeId;
    return value.replace('-', ' ');
  }

  private static String formatRate(long bytesPerSecond) {
    return formatBytes(bytesPerSecond) + "/s";
  }

  private static String formatSignedBytes(long bytes) {
    if (bytes == 0L) return "0 B";
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
    if (unit == 0) return Long.toString(Math.max(0L, bytes)) + " " + units[unit];
    return String.format(
        Locale.ROOT,
        value >= 100.0d ? "%.0f %s" : value >= 10.0d ? "%.1f %s" : "%.2f %s",
        value,
        units[unit]);
  }

  private static String formatDuration(long nanos) {
    if (nanos < 1_000_000L) return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0d);
    if (nanos < 1_000_000_000L) return String.format(Locale.ROOT, "%.0f ms", nanos / 1_000_000.0d);
    return String.format(Locale.ROOT, "%.2f s", nanos / 1_000_000_000.0d);
  }
}
