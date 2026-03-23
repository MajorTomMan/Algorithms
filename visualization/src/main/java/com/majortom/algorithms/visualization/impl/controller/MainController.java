package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.visualization.module.AlgorithmModuleDefinition;
import com.majortom.algorithms.visualization.module.ModuleRegistry;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private StackPane visualizationContainer;
    @FXML
    private HBox customControlBox;
    @FXML
    private VBox moduleMenuBox;
    @FXML
    private Label menuTitleLabel;
    @FXML
    private Label statsTitleLabel;
    @FXML
    private Label logTitleLabel;
    @FXML
    private Label statsLabel;
    @FXML
    private Label delayLabel;
    @FXML
    private Label timelineLabel;
    @FXML
    private TextArea logArea;
    @FXML
    private Button startBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button resetBtn;
    @FXML
    private Button replayBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button compareBtn;
    @FXML
    private Button langBtn;
    @FXML
    private Slider delaySlider;
    @FXML
    private Slider timelineSlider;

    private BaseController<?> currentSubController;
    private final List<AlgorithmModuleDefinition> moduleDefinitions = ModuleRegistry.defaults();
    private final Map<String, Button> moduleButtons = new LinkedHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupModuleMenu();
        setupGlobalEffects();

        if (!moduleDefinitions.isEmpty()) {
            switchToModule(moduleDefinitions.getFirst());
        }

        appendSystemLog(I18N.text("message.system.initialized"));
    }

    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("label.menu.title"));
        statsTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.stats"));
        logTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.log"));
        startBtn.textProperty().bind(I18N.createStringBinding("action.execution.start"));
        resetBtn.textProperty().bind(I18N.createStringBinding("action.execution.reset"));
        replayBtn.textProperty().bind(I18N.createStringBinding("action.execution.replay"));
        exportBtn.textProperty().bind(I18N.createStringBinding("action.execution.export"));
        compareBtn.textProperty().bind(I18N.createStringBinding("action.execution.compare"));
        delayLabel.textProperty().bind(I18N.createStringBinding("label.execution.delay"));
        timelineLabel.textProperty().bind(I18N.createStringBinding("label.execution.timeline"));

        pauseBtn.textProperty().bind(Bindings.createStringBinding(() -> {
            boolean paused = AlgorithmThreadManager.isPaused();
            return I18N.text(paused ? "action.execution.resume" : "action.execution.pause");
        }, I18N.localeProperty()));
    }

    private void setupModuleMenu() {
        moduleMenuBox.getChildren().clear();
        moduleButtons.clear();

        for (AlgorithmModuleDefinition definition : moduleDefinitions) {
            Button button = new Button();
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("menu-button");
            button.getStyleClass().add("module-button");
            button.getStyleClass().add(moduleAccentStyleClass(definition.id()));
            button.textProperty().bind(I18N.createStringBinding(definition.labelKey()));
            button.setOnAction(event -> switchToModule(definition));
            moduleMenuBox.getChildren().add(button);
            moduleButtons.put(definition.id(), button);
        }
    }

    private void setupGlobalEffects() {
        EffectUtils.applyDynamicEffect(startBtn, pauseBtn, resetBtn, replayBtn, exportBtn, compareBtn, langBtn);
    }

    private void switchToModule(AlgorithmModuleDefinition definition) {
        loadSubController(definition.controllerFactory().get());
        moduleButtons.forEach((id, button) -> button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), id.equals(definition.id())));
    }

    private void loadSubController(BaseController<?> newController) {
        AlgorithmThreadManager.stopAll();

        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();

        newController.setUIReferences(
                statsLabel,
                logArea,
                delaySlider,
                timelineSlider,
                customControlBox,
                startBtn,
                pauseBtn,
                resetBtn,
                replayBtn,
                exportBtn,
                compareBtn);

        this.currentSubController = newController;

        BaseVisualizer<?> visualizer = newController.getVisualizer();
        if (visualizer != null) {
            visualizer.prefWidthProperty().bind(visualizationContainer.widthProperty());
            visualizer.prefHeightProperty().bind(visualizationContainer.heightProperty());
            visualizationContainer.getChildren().add(visualizer);
        }

        newController.setupCustomControls(customControlBox);
    }

    @FXML
    private void toggleLanguage() {
        Locale newLocale = I18N.getLocale().getLanguage().equals("zh") ? Locale.ENGLISH : Locale.CHINESE;
        I18N.setLocale(newLocale);
        appendSystemLog(I18N.text("message.system.language_switched", newLocale.getDisplayLanguage(newLocale)));
    }

    private void appendSystemLog(String msg) {
        if (logArea != null) {
            logArea.appendText("System: " + msg + "\n");
        }
    }

    private String moduleAccentStyleClass(String moduleId) {
        return switch (moduleId) {
            case "sort" -> "btn-ran-blue";
            case "maze" -> "btn-ran-red";
            case "tree" -> "btn-ran-gold";
            case "graph" -> "btn-ran-purple";
            default -> "btn-ran-blue";
        };
    }
}
