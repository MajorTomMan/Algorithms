package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.metrics.MetricItem;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewModel;
import com.majortom.algorithms.visualization.metrics.RuntimeOverviewText;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import java.util.List;
import javafx.scene.control.Label;

/** Owns compact and expanded structure overview labels without retaining editable structure state. */
final class WorkbenchStructureOverview {
    private final Label structureOverviewLabel;
    private final Label structurePrimaryMetricTitleLabel;
    private final Label structureNodeCountLabel;
    private final Label structureSecondaryMetricTitleLabel;
    private final Label structureHeightLabel;
    private final Label structureStateLabel;
    private final Label overviewPrimaryTitleLabel;
    private final Label overviewPrimaryValue;
    private final Label overviewSecondaryTitleLabel;
    private final Label overviewSecondaryValue;
    private final Label overviewEventsTitleLabel;
    private final Label overviewEventsValue;
    private final Label overviewStateValue;

    WorkbenchStructureOverview(Label structureOverviewLabel, Label structurePrimaryMetricTitleLabel, Label structureNodeCountLabel, Label structureSecondaryMetricTitleLabel, Label structureHeightLabel, Label structureStateLabel, Label overviewPrimaryTitleLabel, Label overviewPrimaryValue, Label overviewSecondaryTitleLabel, Label overviewSecondaryValue, Label overviewEventsTitleLabel, Label overviewEventsValue, Label overviewStateValue) {
        this.structureOverviewLabel = structureOverviewLabel;
        this.structurePrimaryMetricTitleLabel = structurePrimaryMetricTitleLabel;
        this.structureNodeCountLabel = structureNodeCountLabel;
        this.structureSecondaryMetricTitleLabel = structureSecondaryMetricTitleLabel;
        this.structureHeightLabel = structureHeightLabel;
        this.structureStateLabel = structureStateLabel;
        this.overviewPrimaryTitleLabel = overviewPrimaryTitleLabel;
        this.overviewPrimaryValue = overviewPrimaryValue;
        this.overviewSecondaryTitleLabel = overviewSecondaryTitleLabel;
        this.overviewSecondaryValue = overviewSecondaryValue;
        this.overviewEventsTitleLabel = overviewEventsTitleLabel;
        this.overviewEventsValue = overviewEventsValue;
        this.overviewStateValue = overviewStateValue;
    }

    void renderPreview(StructureSnapshotSupport<?> snapshotSupport,
            StructureSnapshot<?> previewSnapshot, String description) {
        String primary = snapshotPrimaryCount(snapshotSupport, previewSnapshot);
        String secondary = snapshotSecondaryCount(snapshotSupport, previewSnapshot);
        if (structureOverviewLabel != null) structureOverviewLabel.setText(description);
        if (structureNodeCountLabel != null) structureNodeCountLabel.setText(primary);
        if (structureHeightLabel != null) structureHeightLabel.setText(secondary);
        if (structureStateLabel != null) structureStateLabel.setText(I18N.text("status.workspace.preview"));
        if (overviewPrimaryValue != null) overviewPrimaryValue.setText(primary);
        if (overviewSecondaryValue != null) overviewSecondaryValue.setText(secondary);
        if (overviewEventsValue != null) overviewEventsValue.setText("—");
        if (overviewStateValue != null) overviewStateValue.setText(I18N.text("status.workspace.preview"));
    }

    void renderLive(RuntimeOverviewModel overview, String summary) {
        if (structureOverviewLabel != null) {
            structureOverviewLabel.setText(summary == null || summary.isBlank()
                    ? I18N.text("status.workspace.ready") : summary);
        }
        List<MetricItem> structureMetrics = overview.structureMetrics();
        applyStructureMetric(0, structurePrimaryMetricTitleLabel, structureNodeCountLabel,
                overviewPrimaryTitleLabel, overviewPrimaryValue, structureMetrics);
        applyStructureMetric(1, structureSecondaryMetricTitleLabel, structureHeightLabel,
                overviewSecondaryTitleLabel, overviewSecondaryValue, structureMetrics);
        applyStructureMetric(2, null, null, overviewEventsTitleLabel, overviewEventsValue, structureMetrics);
        if (structureStateLabel != null) structureStateLabel.setText(I18N.text("status.workspace.ready"));
        if (overviewStateValue != null) overviewStateValue.setText(I18N.text("status.workspace.ready"));
    }

    private void applyStructureMetric(
            int index,
            Label compactTitle,
            Label compactValue,
            Label overviewTitle,
            Label overviewValue,
            List<MetricItem> metrics) {
        if (index < 0 || index >= metrics.size()) {
            if (compactValue != null) compactValue.setText("—");
            if (overviewValue != null) overviewValue.setText("—");
            return;
        }
        MetricItem metric = metrics.get(index);
        String title = RuntimeOverviewText.label(metric);
        String value = RuntimeOverviewText.value(metric);
        if (compactTitle != null) compactTitle.setText(title);
        if (compactValue != null) compactValue.setText(value);
        if (overviewTitle != null) overviewTitle.setText(title);
        if (overviewValue != null) overviewValue.setText(value);
    }

    @SuppressWarnings("unchecked")
    private String snapshotPrimaryCount(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        return typedSupport.snapshotPrimaryCount(typedSnapshot.state());
    }

    @SuppressWarnings("unchecked")
    private String snapshotSecondaryCount(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        return typedSupport.snapshotSecondaryCount(typedSnapshot.state());
    }

}
