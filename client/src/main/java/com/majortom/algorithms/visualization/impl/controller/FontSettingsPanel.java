package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.settings.FontSettings;
import com.majortom.algorithms.visualization.settings.FontSettingsService;
import com.majortom.algorithms.visualization.settings.FontSettingsPopupPlacement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/** Builds and owns the font settings popup; the Workbench owns global application of settings. */
final class FontSettingsPanel {
    private final BorderPane rootPane;
    private final Button fontSettingsBtn;
    private final FontSettingsService service;
    private final BiConsumer<FontSettings, Locale> onApply;
    private Popup popup;

    FontSettingsPanel(BorderPane rootPane, Button fontSettingsBtn, FontSettingsService service,
            BiConsumer<FontSettings, Locale> onApply) {
        this.rootPane = rootPane;
        this.fontSettingsBtn = fontSettingsBtn;
        this.service = service;
        this.onApply = onApply;
    }

    void hide() {
        if (popup != null && popup.isShowing()) popup.hide();
    }

    void toggle(FontSettings appliedFontSettings) {
        if (fontSettingsBtn == null) return;
        if (popup != null && popup.isShowing()) { popup.hide(); return; }
        Bounds anchor = fontSettingsBtn.localToScreen(fontSettingsBtn.getBoundsInLocal());
        Bounds window = rootPane.localToScreen(rootPane.getBoundsInLocal());
        if (anchor == null || window == null) return;
        Popup next = createFontSettingsPopup(appliedFontSettings);
        popup = next;
        next.setOnHidden(event -> { if (popup == next) popup = null; });
        FontSettingsPopupPlacement.show(next, fontSettingsBtn, anchor, window);
    }

    private Popup createFontSettingsPopup(FontSettings appliedFontSettings) {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setAnchorLocation(javafx.stage.PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);

        VBox popupShell = new VBox();
        popupShell.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        popupShell.getStyleClass().add("font-settings-popup-shell");
        popupShell.getStylesheets().addAll(rootPane.getStylesheets());

        Region arrow = new Region();
        arrow.getStyleClass().add("font-settings-arrow");
        HBox arrowRow = new HBox(arrow);
        arrowRow.getStyleClass().add("font-settings-arrow-row");

        VBox content = new VBox();
        content.getStyleClass().add("font-settings-popover");

        FontSettings initial = appliedFontSettings;
        if (initial == null) {
            initial = service.load();
        }
        FontSettings[] draft = new FontSettings[] { initial };
        Locale[] draftLocale = new Locale[] { I18N.getLocale() };
        boolean[] updatingControls = new boolean[] { false };

        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding("settings.text.title"));
        title.getStyleClass().add("font-settings-title");

        Label languageLabel = new Label();
        languageLabel.textProperty().bind(I18N.createStringBinding("settings.language"));
        languageLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> languageSelector = new ComboBox<>();
        languageSelector.setMaxWidth(Double.MAX_VALUE);
        languageSelector.getStyleClass().addAll("font-settings-combo", "font-settings-language-combo");
        languageSelector.getItems().setAll("中文", "English");
        if (I18N.getLocale().getLanguage().equals("zh")) {
            languageSelector.getSelectionModel().select("中文");
        } else {
            languageSelector.getSelectionModel().select("English");
        }
        HBox languageRow = fontSettingsRow(languageLabel, languageSelector);

        String projectDefault = I18N.text("settings.text.fontDefault");
        List<String> families = new ArrayList<>();
        families.add(projectDefault);
        families.addAll(service.availableFamilies());

        Label chineseFamilyLabel = new Label();
        chineseFamilyLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontChinese"));
        chineseFamilyLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> chineseFamilySelector = new ComboBox<>();
        chineseFamilySelector.setMaxWidth(Double.MAX_VALUE);
        chineseFamilySelector.getStyleClass().add("font-settings-combo");
        chineseFamilySelector.getItems().setAll(families);
        chineseFamilySelector.getSelectionModel().select(
                initial.chineseFamily().isBlank() ? projectDefault : initial.chineseFamily());
        HBox chineseFamilyRow = fontSettingsRow(chineseFamilyLabel, chineseFamilySelector);

        Label englishFamilyLabel = new Label();
        englishFamilyLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontEnglish"));
        englishFamilyLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> englishFamilySelector = new ComboBox<>();
        englishFamilySelector.setMaxWidth(Double.MAX_VALUE);
        englishFamilySelector.getStyleClass().add("font-settings-combo");
        englishFamilySelector.getItems().setAll(families);
        englishFamilySelector.getSelectionModel().select(
                initial.englishFamily().isBlank() ? projectDefault : initial.englishFamily());
        HBox englishFamilyRow = fontSettingsRow(englishFamilyLabel, englishFamilySelector);

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontSize"));
        sizeLabel.getStyleClass().add("font-settings-row-label");
        Button decreaseSize = new Button("−");
        decreaseSize.getStyleClass().add("font-settings-size-button");
        decreaseSize.setAccessibleText(I18N.text("settings.text.decrease"));
        Button increaseSize = new Button("+");
        increaseSize.getStyleClass().add("font-settings-size-button");
        increaseSize.setAccessibleText(I18N.text("settings.text.increase"));
        Label sizeValue = new Label(formatFontSize(initial.size()));
        sizeValue.setMaxWidth(Double.MAX_VALUE);
        sizeValue.getStyleClass().add("font-settings-size-value");
        HBox.setHgrow(sizeValue, Priority.ALWAYS);
        HBox sizeControl = new HBox(decreaseSize, sizeValue, increaseSize);
        sizeControl.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        sizeControl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sizeControl, Priority.ALWAYS);
        sizeControl.getStyleClass().add("font-settings-size-control");
        HBox sizeRow = fontSettingsRow(sizeLabel, sizeControl);

        Label colorLabel = new Label();
        colorLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontColor"));
        colorLabel.getStyleClass().add("font-settings-row-label");
        ColorPicker colorPicker = new ColorPicker(service.colorForPicker(initial));
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.getStyleClass().add("font-settings-color-picker");
        HBox colorRow = fontSettingsRow(colorLabel, colorPicker);

        Label previewTitle = new Label();
        previewTitle.textProperty().bind(I18N.createStringBinding("settings.text.preview"));
        previewTitle.getStyleClass().add("font-settings-preview-title");
        Label previewPrimary = new Label();
        previewPrimary.textProperty().bind(I18N.createStringBinding("settings.text.preview.primary"));
        previewPrimary.getStyleClass().add("font-settings-preview-primary");
        Label previewArray = new Label();
        previewArray.textProperty().bind(I18N.createStringBinding("settings.text.preview.array"));
        Label previewStep = new Label();
        previewStep.textProperty().bind(I18N.createStringBinding("settings.text.preview.step"));
        VBox preview = new VBox(previewPrimary, previewArray, previewStep);
        preview.getStyleClass().add("font-settings-preview");

        Runnable refreshPreview = () -> service.applyPreview(preview, draft[0]);

        languageSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            Locale selectedLocale = Locale.CHINESE;
            if ("English".equals(newValue)) {
                selectedLocale = Locale.ENGLISH;
            }
            draftLocale[0] = selectedLocale;
        });

        chineseFamilySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            String family = chineseFamilySelector.getSelectionModel().getSelectedIndex() == 0 ? "" : newValue;
            draft[0] = new FontSettings(
                    family, draft[0].englishFamily(), draft[0].size(), draft[0].color());
            refreshPreview.run();
        });
        englishFamilySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            String family = englishFamilySelector.getSelectionModel().getSelectedIndex() == 0 ? "" : newValue;
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), family, draft[0].size(), draft[0].color());
            refreshPreview.run();
        });
        decreaseSize.setOnAction(event -> {
            double size = service.clampSize(draft[0].size() - 1.0d);
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), draft[0].englishFamily(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        increaseSize.setOnAction(event -> {
            double size = service.clampSize(draft[0].size() + 1.0d);
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), draft[0].englishFamily(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(),
                    draft[0].englishFamily(),
                    draft[0].size(),
                    service.toCssColor(newValue));
            refreshPreview.run();
        });

        Button reset = new Button();
        reset.textProperty().bind(I18N.createStringBinding("settings.text.reset"));
        reset.getStyleClass().add("font-settings-reset-button");
        reset.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(reset, Priority.ALWAYS);
        Button apply = new Button();
        apply.textProperty().bind(I18N.createStringBinding("settings.text.apply"));
        apply.getStyleClass().add("font-settings-apply-button");
        HBox footer = new HBox(reset, apply);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.getStyleClass().add("font-settings-footer");

        reset.setOnAction(event -> {
            FontSettings defaults = service.defaults();
            updatingControls[0] = true;
            chineseFamilySelector.getSelectionModel().select(projectDefault);
            englishFamilySelector.getSelectionModel().select(projectDefault);
            sizeValue.setText(formatFontSize(defaults.size()));
            colorPicker.setValue(service.colorForPicker(defaults));
            draftLocale[0] = I18N.getLocale();
            if ("zh".equals(draftLocale[0].getLanguage())) {
                languageSelector.getSelectionModel().select("中文");
            } else {
                languageSelector.getSelectionModel().select("English");
            }
            updatingControls[0] = false;
            draft[0] = defaults;
            refreshPreview.run();
        });
        apply.setOnAction(event -> {
            FontSettings normalized = service.normalize(draft[0]);
            Locale selectedLocale = draftLocale[0];
            service.save(normalized);
            onApply.accept(normalized, selectedLocale);
            popup.hide();
        });

        content.getChildren().setAll(
                title,
                languageRow,
                chineseFamilyRow,
                englishFamilyRow,
                sizeRow,
                colorRow,
                new Separator(),
                previewTitle,
                preview,
                new Separator(),
                footer);
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("font-settings-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        popupShell.getChildren().setAll(arrowRow, scroll);
        service.apply(popupShell, initial);
        service.applyPreview(preview, initial);
        WorkbenchTheme.apply(popupShell);
        popup.getContent().setAll(popupShell);
        return popup;
    }

    private HBox fontSettingsRow(Label label, Node control) {
        label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox row = new HBox(label, control);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("font-settings-row");
        return row;
    }

    private String formatFontSize(double size) {
        double rounded = Math.rint(size);
        if (Math.abs(size - rounded) < 0.001d) {
            return String.format(Locale.ROOT, "%.0f px", rounded);
        }
        return String.format(Locale.ROOT, "%.2f px", size);
    }

}
