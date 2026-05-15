package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.BaseTreeAlgorithms;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmFamily;
import com.majortom.algorithms.visualization.algorithm.AlgorithmRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmStructure;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 树算法模块控制器。
 *
 * <p>底栏统一为结构、算法和操作入口；插入、删除和随机填值放到操作弹窗里，
 * 避免默认界面长期占用多个按钮和输入框。</p>
 */
public class TreeController<T extends Comparable<T>> extends BaseModuleController<BaseTree<T>> {

    /**
     * 树操作类型。
     */
    private enum Mode {
        INSERT, DELETE
    }

    private final BaseTree<T> treeData;
    private AlgorithmStructure currentStructure = AlgorithmStructure.AVL_TREE;
    private Mode currentMode = Mode.INSERT;
    private String pendingInput;
    private String currentAlgorithmId;

    @FXML
    private Label structureLabel;
    @FXML
    private ComboBox<String> structureSelector;
    @FXML
    private Label algorithmLabel;
    @FXML
    private ComboBox<String> algorithmSelector;
    @FXML
    private Button operationBtn;

    /**
     * 创建树模块控制器。
     *
     * @param treeData 树结构实体
     * @param algorithm 默认算法实例；保留构造签名兼容模块注册，实际下拉由注册表驱动
     */
    public TreeController(BaseTree<T> treeData, BaseTreeAlgorithms<T> algorithm) {
        super(new TreeVisualizer<>(), "/fxml/TreeControls.fxml");
        this.treeData = treeData;
    }

    /**
     * 初始化结构、算法下拉框和初始渲染。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindStructureSelector();
        bindAlgorithmSelector();

        if (visualizer != null && treeData != null) {
            visualizer.render(treeData);
        }

        EffectUtils.applyDynamicEffect(operationBtn);
    }

    /**
     * 打开树操作弹窗。
     */
    @FXML
    private void openTreeOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.tree.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField valueField = new TextField();
        valueField.setPromptText(I18N.text("prompt.tree.value"));
        valueField.getStyleClass().addAll("dark-textfield", "dialog-input");

        Button insertButton = new Button(I18N.text("action.tree.insert"));
        insertButton.getStyleClass().addAll("btn-ran-blue", "compact-button");
        insertButton.setOnAction(event -> runTreeOperation(Mode.INSERT, valueField.getText()));

        Button deleteButton = new Button(I18N.text("action.tree.delete"));
        deleteButton.getStyleClass().addAll("btn-ran-red", "compact-button");
        deleteButton.setOnAction(event -> runTreeOperation(Mode.DELETE, valueField.getText()));

        Button randomButton = new Button(I18N.text("action.tree.random"));
        randomButton.getStyleClass().addAll("btn-ran-gold", "compact-button");
        randomButton.setOnAction(event -> valueField.setText(String.valueOf((int) (Math.random() * 100))));

        HBox actions = new HBox(10, insertButton, deleteButton, randomButton);
        actions.getStyleClass().add("dialog-action-row");

        VBox content = new VBox(12,
                new Label(I18N.text("label.tree.value")),
                valueField,
                actions);
        content.getStyleClass().add("operation-dialog-content");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style/ui_theme.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("operation-dialog-pane");
        dialog.getDialogPane().setPrefWidth(540);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    /**
     * 响应全局 Start 按钮。
     */
    @Override
    public void handleAlgorithmStart() {
        BaseTreeAlgorithms<T> algorithm = selectedAlgorithm();

        if (treeData != null && algorithm != null && !AlgorithmThreadManager.isRunning()) {
            startAlgorithm(algorithm, treeData);
        }
    }

    /**
     * 算法核心执行逻辑：支持单值或逗号分隔的批量操作。
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseTree<T>> alg, BaseTree<T> tree) {
        String input = pendingInput;
        if (input == null || input.trim().isEmpty() || !(alg instanceof BaseTreeAlgorithms)) {
            appendLog(I18N.text("message.error.invalid_tree_input"));
            return;
        }

        BaseTreeAlgorithms<T> targetAlg = (BaseTreeAlgorithms<T>) alg;
        String[] values = input.split("[,，]");

        for (String valStr : values) {
            if (!AlgorithmThreadManager.isRunning()) {
                break;
            }

            try {
                T val = parseValue(valStr.trim());
                if (currentMode == Mode.INSERT) {
                    targetAlg.put(tree, val);
                    logI18n("message.tree.insert", val);
                } else {
                    targetAlg.remove(tree, val);
                    logI18n("message.tree.delete", val);
                }
            } catch (Exception e) {
                appendLog(I18N.text("message.error.value_process", valStr));
            }
        }
    }

    /**
     * 格式化统计信息：增加树的高度显示。
     */
    @Override
    protected String formatStatsMessage() {
        if (treeData == null) {
            return I18N.text("stats.tree.empty");
        }
        return String.format("%s | %s | %s",
                I18N.text("stats.size", treeData.size()),
                I18N.text("stats.height", treeData.height()),
                formatMetric("stats.action", stats.actionCount()));
    }

    /**
     * 重置树数据的统计和高亮。
     */
    @Override
    protected void onResetData() {
        AlgorithmThreadManager.stopAll();

        if (treeData != null) {
            treeData.resetStatistics();
            visualizer.render(treeData, null, null);
            appendLog(I18N.text("message.tree.reset"));
        }
    }

    /**
     * 绑定国际化文案。
     */
    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (operationBtn != null) {
            operationBtn.textProperty().bind(I18N.createStringBinding("action.tree.operation"));
        }
    }

    /**
     * 获取模块 ID。
     */
    @Override
    protected String moduleId() {
        return "tree";
    }

    /**
     * 返回当前选中算法的稳定 ID。
     */
    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseTree<T>> algorithm) {
        return currentAlgorithmId == null ? super.executionAlgorithmId(algorithm) : currentAlgorithmId;
    }

    /**
     * 运行弹窗里选择的树操作。
     */
    private void runTreeOperation(Mode mode, String input) {
        this.currentMode = mode;
        this.pendingInput = input;
        handleAlgorithmStart();
    }

    /**
     * 绑定结构下拉框。
     */
    private void bindStructureSelector() {
        if (structureSelector == null) {
            return;
        }

        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            list.add(I18N.text("label.tree.structure.avl"));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }

    /**
     * 绑定算法下拉框。
     */
    private void bindAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }

        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            treeOptions().forEach(option -> list.add(I18N.text(option.labelKey())));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> algorithmSelector.getSelectionModel().selectFirst());
    }

    /**
     * 查询当前树结构下可用算法。
     */
    private List<AlgorithmDescriptor> treeOptions() {
        return AlgorithmRegistry.find(moduleId(), AlgorithmFamily.TREE_OPERATION, currentStructure);
    }

    /**
     * 创建当前选中的树算法。
     */
    @SuppressWarnings("unchecked")
    private BaseTreeAlgorithms<T> selectedAlgorithm() {
        List<AlgorithmDescriptor> options = treeOptions();
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= options.size()) {
            currentAlgorithmId = null;
            return null;
        }

        AlgorithmDescriptor option = options.get(index);
        currentAlgorithmId = option.id();
        return (BaseTreeAlgorithms<T>) option.create();
    }

    /**
     * 按当前业务默认把文本解析为 Integer。
     */
    @SuppressWarnings("unchecked")
    private T parseValue(String s) {
        return (T) Integer.valueOf(s);
    }
}
