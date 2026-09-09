package com.majortom.algorithms.visualization.settings;

import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/** Owns loading, validation, persistence and CSS application of font display preferences. */
public final class FontSettingsService {

    public enum LayoutTier {
        NORMAL,
        LARGE,
        XLARGE
    }

    public static final double MIN_SIZE = 10.0d;
    public static final double MAX_SIZE = 24.0d;

    private static final double LEGACY_BASE_SIZE = 13.0d;
    private static final int LEGACY_DEFAULT_SCALE = 125;
    private static final double PROJECT_DEFAULT_SIZE = LEGACY_BASE_SIZE * LEGACY_DEFAULT_SCALE / 100.0d;
    private static final String PROJECT_DEFAULT_PREVIEW_COLOR = "#F2F3F4";
    private static final String KEY_FAMILY = "ui.font.family";
    private static final String KEY_SIZE = "ui.font.size";
    private static final String KEY_COLOR = "ui.font.color";
    private static final String LEGACY_SCALE_KEY = "ui.font.scale";
    private static final String CUSTOM_COLOR_CLASS = "font-color-custom";
    private static final String LARGE_FONT_CLASS = "font-size-large";
    private static final String XLARGE_FONT_CLASS = "font-size-xlarge";
    private static final double LARGE_FONT_THRESHOLD = 18.0d;
    private static final double XLARGE_FONT_THRESHOLD = 21.0d;
    private static final String TYPOGRAPHY_STYLESHEET = "/style/typography.css";
    private static final Preferences PREFERENCES = Preferences.userRoot().node(
            "/com/majortom/algorithms/visualization/impl/controller");

    public FontSettings defaults() {
        return new FontSettings("", PROJECT_DEFAULT_SIZE, "");
    }

    public FontSettings load() {
        String family = PREFERENCES.get(KEY_FAMILY, "");
        double size;
        if (PREFERENCES.get(KEY_SIZE, null) == null) {
            int legacyScale = PREFERENCES.getInt(LEGACY_SCALE_KEY, LEGACY_DEFAULT_SCALE);
            size = LEGACY_BASE_SIZE * legacyScale / 100.0d;
        } else {
            size = PREFERENCES.getDouble(KEY_SIZE, PROJECT_DEFAULT_SIZE);
        }
        String color = PREFERENCES.get(KEY_COLOR, "");
        return normalize(new FontSettings(family, size, color));
    }

    public void save(FontSettings settings) {
        FontSettings normalized = normalize(settings);
        PREFERENCES.put(KEY_FAMILY, normalized.family());
        PREFERENCES.putDouble(KEY_SIZE, normalized.size());
        PREFERENCES.put(KEY_COLOR, normalized.color());
    }

    public void apply(Parent root, FontSettings settings) {
        if (root == null) {
            return;
        }
        FontSettings normalized = normalize(settings);
        ensureTypographyStylesheet(root);
        root.setStyle(mergeManagedStyle(root.getStyle(), normalized, false));
        setCustomColorClass(root, !normalized.color().isBlank());
        setFontSizeClass(root, normalized.size());
    }

    /** Applies a draft only to preview content; application preferences are unchanged. */
    public void applyPreview(Parent previewRoot, FontSettings settings) {
        if (previewRoot == null) {
            return;
        }
        FontSettings normalized = normalize(settings);
        ensureTypographyStylesheet(previewRoot);
        String family = normalized.family();
        if (family.isBlank()) {
            family = projectDefaultFamily();
        }
        FontSettings previewSettings = new FontSettings(family, normalized.size(), normalized.color());
        previewRoot.setStyle(mergeManagedStyle(previewRoot.getStyle(), previewSettings, true));
        setCustomColorClass(previewRoot, !previewSettings.color().isBlank());
    }

    public FontSettings normalize(FontSettings settings) {
        FontSettings source = settings;
        if (source == null) {
            source = defaults();
        }
        String family = normalizeFamily(source.family());
        double size = clampSize(source.size());
        String color = normalizeColor(source.color());
        return new FontSettings(family, size, color);
    }

    public double clampSize(double size) {
        if (!Double.isFinite(size)) {
            return PROJECT_DEFAULT_SIZE;
        }
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
    }

    public LayoutTier layoutTier(FontSettings settings) {
        FontSettings normalized = normalize(settings);
        return layoutTier(normalized.size());
    }

    public LayoutTier layoutTier(double size) {
        double normalizedSize = clampSize(size);
        if (normalizedSize >= XLARGE_FONT_THRESHOLD) {
            return LayoutTier.XLARGE;
        }
        if (normalizedSize >= LARGE_FONT_THRESHOLD) {
            return LayoutTier.LARGE;
        }
        return LayoutTier.NORMAL;
    }

    public List<String> availableFamilies() {
        return List.copyOf(Font.getFamilies());
    }

    public Color colorForPicker(FontSettings settings) {
        FontSettings normalized = normalize(settings);
        if (normalized.color().isBlank()) {
            return Color.web(PROJECT_DEFAULT_PREVIEW_COLOR);
        }
        return Color.web(normalized.color());
    }

    public String toCssColor(Color color) {
        if (color == null) {
            return "";
        }
        int red = (int) Math.round(color.getRed() * 255.0d);
        int green = (int) Math.round(color.getGreen() * 255.0d);
        int blue = (int) Math.round(color.getBlue() * 255.0d);
        int alpha = (int) Math.round(color.getOpacity() * 255.0d);
        if (alpha >= 255) {
            return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
        }
        return String.format(Locale.ROOT, "#%02X%02X%02X%02X", red, green, blue, alpha);
    }

    private String normalizeFamily(String family) {
        if (family == null || family.isBlank()) {
            return "";
        }
        if (Font.getFamilies().contains(family)) {
            return family;
        }
        return "";
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "";
        }
        try {
            return toCssColor(Color.web(color));
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private String projectDefaultFamily() {
        List<String> families = Font.getFamilies();
        List<String> preferred = List.of("Segoe UI", "Microsoft YaHei", "Arial", "Consolas");
        for (String family : preferred) {
            if (families.contains(family)) {
                return family;
            }
        }
        return Font.getDefault().getFamily();
    }

    private String mergeManagedStyle(String existing, FontSettings settings, boolean preview) {
        StringBuilder result = new StringBuilder();
        if (existing != null && !existing.isBlank()) {
            for (String declaration : existing.split(";")) {
                String trimmed = declaration.trim();
                if (trimmed.isBlank() || managedDeclaration(trimmed)) {
                    continue;
                }
                result.append(trimmed).append(';');
            }
        }
        if (!settings.family().isBlank()) {
            result.append("-fx-font-family: \"")
                    .append(escapeCssString(settings.family()))
                    .append("\";");
        }
        result.append(String.format(Locale.ROOT, "-fx-font-size: %.2fpx;", settings.size()));
        if (!settings.color().isBlank()) {
            result.append("-ui-user-text: ").append(settings.color()).append(';');
        } else if (preview) {
            result.append("-ui-user-text: ").append(PROJECT_DEFAULT_PREVIEW_COLOR).append(';');
        }
        return result.toString();
    }

    private boolean managedDeclaration(String declaration) {
        return declaration.startsWith("-fx-font-family:")
                || declaration.startsWith("-fx-font-size:")
                || declaration.startsWith("-ui-user-text:");
    }

    private String escapeCssString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    private void ensureTypographyStylesheet(Parent root) {
        java.net.URL resource = FontSettingsService.class.getResource(TYPOGRAPHY_STYLESHEET);
        if (resource == null) {
            return;
        }
        String stylesheet = resource.toExternalForm();
        if (!root.getStylesheets().contains(stylesheet)) {
            root.getStylesheets().add(stylesheet);
        }
    }

    private void setFontSizeClass(Parent root, double size) {
        root.getStyleClass().removeAll(LARGE_FONT_CLASS, XLARGE_FONT_CLASS);
        LayoutTier tier = layoutTier(size);
        if (tier == LayoutTier.XLARGE) {
            root.getStyleClass().add(XLARGE_FONT_CLASS);
            return;
        }
        if (tier == LayoutTier.LARGE) {
            root.getStyleClass().add(LARGE_FONT_CLASS);
        }
    }

    private void setCustomColorClass(Parent root, boolean enabled) {
        root.getStyleClass().remove(CUSTOM_COLOR_CLASS);
        if (enabled) {
            root.getStyleClass().add(CUSTOM_COLOR_CLASS);
        }
    }
}
