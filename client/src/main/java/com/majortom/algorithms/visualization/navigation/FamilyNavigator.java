package com.majortom.algorithms.visualization.navigation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.Region;

/**
 * Stable family navigation chrome.
 *
 * <p>Only registry/locale/typography/viewport changes are allowed to affect geometry.
 * Selection, hover and disabled state are presentation-only.</p>
 */
public final class FamilyNavigator extends Region {
  private static final double ROW_GAP_EM = 0.08d;
  private static final double MIN_WIDTH_EM = 11.0d;
  private static final double MAX_WIDTH_EM = 16.0d;

  private final List<FamilyItemView> items = new ArrayList<>();
  private final Map<String, FamilyItemView> byId = new LinkedHashMap<>();
  private double em = 16.0d;
  private String selectedFamilyId;

  public FamilyNavigator() {
    getStyleClass().add("family-navigator");
  }

  public void setEntries(List<FamilyEntry> entries) {
    for (FamilyItemView item : items) {
      item.dispose();
    }
    items.clear();
    byId.clear();
    getChildren().clear();

    if (entries != null) {
      for (int index = 0; index < entries.size(); index++) {
        FamilyEntry entry = entries.get(index);
        FamilyItemView item = new FamilyItemView(entry, "%02d".formatted(index + 1));
        item.setEm(em);
        item.setSelected(entry.id().equals(selectedFamilyId));
        items.add(item);
        byId.put(entry.id(), item);
        getChildren().add(item);
      }
    }
    requestLayout();
  }

  /** Single typography entry point for the whole navigator. */
  public void applyTypography(double fontSizePx) {
    double resolved = Math.max(8.0d, fontSizePx);
    if (Math.abs(em - resolved) < 0.01d) {
      return;
    }
    em = resolved;
    setStyle("-fx-font-size: " + resolved + "px;");
    for (FamilyItemView item : items) {
      item.setEm(resolved);
    }
    requestLayout();
  }

  public void setSelectedFamily(String familyId) {
    selectedFamilyId = familyId;
    byId.forEach((id, item) -> item.setSelected(id.equals(familyId)));
  }

  public String selectedFamily() {
    return selectedFamilyId;
  }

  public void setFamilyDisabled(String familyId, boolean disabled) {
    FamilyItemView item = byId.get(familyId);
    if (item != null) {
      item.setDisable(disabled);
    }
  }

  public FamilyItemView item(String familyId) {
    return byId.get(familyId);
  }

  public List<FamilyItemView> items() {
    return List.copyOf(items);
  }

  public double preferredRailWidth() {
    double widest = MIN_WIDTH_EM * em;
    for (FamilyItemView item : items) {
      widest = Math.max(widest, item.preferredWidth());
    }
    return clamp(widest, MIN_WIDTH_EM * em, MAX_WIDTH_EM * em);
  }

  @Override
  protected double computeMinWidth(double height) {
    return MIN_WIDTH_EM * em;
  }

  @Override
  protected double computePrefWidth(double height) {
    return preferredRailWidth();
  }

  @Override
  protected double computeMaxWidth(double height) {
    return MAX_WIDTH_EM * em;
  }

  @Override
  protected double computeMinHeight(double width) {
    return computePrefHeight(width);
  }

  @Override
  protected double computePrefHeight(double width) {
    if (items.isEmpty()) {
      return 0.0d;
    }
    double row = items.getFirst().rowHeight();
    return row * items.size() + ROW_GAP_EM * em * Math.max(0, items.size() - 1);
  }

  @Override
  protected void layoutChildren() {
    double y = 0.0d;
    double gap = ROW_GAP_EM * em;
    double width = getWidth();
    for (int index = 0; index < items.size(); index++) {
      FamilyItemView item = items.get(index);
      double rowHeight = item.rowHeight();
      item.resizeRelocate(0.0d, y, width, rowHeight);
      y += rowHeight;
      if (index + 1 < items.size()) {
        y += gap;
      }
    }
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
