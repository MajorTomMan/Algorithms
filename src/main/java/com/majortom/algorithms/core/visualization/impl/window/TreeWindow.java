package com.majortom.algorithms.core.visualization.impl.window;

import atlantafx.base.theme.Styles;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.visualization.BaseWindow;
import com.majortom.algorithms.core.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 树算法可视化窗口
 */
public class TreeWindow extends BaseWindow<BaseTree<Integer>> {

    private TreeVisualizer<Integer> treeVisualizer;
    private final BaseTree<Integer> algorithm;
    private TextField inputField;

    public TreeWindow(BaseTree<Integer> algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    protected Region createCenterComponent() {
        // 初始化具备插值动画的 TreeVisualizer
        this.treeVisualizer = new TreeVisualizer<>(null);
        return treeVisualizer;
    }

    @Override
    protected void setupActions() {
        // 1. 初始化输入组件（因为 MainView.fxml 只有基础按钮，特定的输入框需动态加入控制栏）
        HBox inputGroup = new HBox(10);
        inputGroup.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("数据:");
        label.setStyle("-fx-text-fill: #888888;");

        inputField = new TextField();
        inputField.setPromptText("例如: 50, 30, 70");
        inputField.setPrefWidth(150);

        Button randomBtn = new Button("随机生成");
        randomBtn.getStyleClass().add(Styles.BUTTON_OUTLINED);

        inputGroup.getChildren().addAll(label, inputField, randomBtn);

        // 2. 将特定组件插入到 FXML 定义的底部控制栏起始位置
        // 注意：controlPanel 在基类中是 HBox 类型 [cite: 2026-01-10]
        // 如果你直接操作 rootPane 的 bottom，可以使用 getBottom() 获取 HBox
        HBox footer = (HBox) rootPane.getBottom();
        footer.getChildren().add(0, inputGroup);

        // 3. 配置基类按钮文字与样式
        startBtn.setText("执行插入");
        startBtn.getStyleClass().add(Styles.ACCENT);

        // 4. 绑定算法监听器到基类的 onSync [cite: 2026-01-10]
        this.algorithm.setListener((data, a, b, comp, act) -> {
            this.onSync(data, a, b, comp, act);
        });

        // 5. 事件响应逻辑
        randomBtn.setOnAction(e -> {
            Integer[] vals = AlgorithmsUtils.randomArray(5, 100);
            String text = Arrays.toString(vals).replaceAll("[\\[\\]]", "");
            inputField.setText(text);
        });

        // 覆盖基类的 handleStartAction 逻辑或直接在 startBtn 重新设置
        startBtn.setOnAction(e -> {
            Integer[] dataToInsert = parseInput();
            if (dataToInsert.length > 0) {
                this.setTask(() -> {
                    for (Integer v : dataToInsert) {
                        algorithm.put(v);
                    }
                });
                this.executeTask();
            }
        });
    }

    @Override
    protected void handleDataReset() {
        if (algorithm != null) {
            algorithm.clear();
            algorithm.resetStatistics();
        }
        treeVisualizer.updateState(null, null, null);
        // 对应 XML 中的 logArea [cite: 2026-01-10]
        logArea.setText("中序遍历: \n[]");
    }

    @Override
    protected void refresh(BaseTree<Integer> treeData, Object activeNode, Object secondaryNode) {
        if (treeData == null)
            return;

        // 更新画布
        treeVisualizer.updateState(treeData.getRoot(), activeNode, secondaryNode);

        // 更新侧边栏日志
        List<Integer> list = treeData.toList();
        logArea.setText("中序遍历: \n" + list.toString());
    }

    private Integer[] parseInput() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty())
            return new Integer[0];

        try {
            String[] parts = text.replace("，", ",").split("[,\\s]+");
            List<Integer> list = new ArrayList<>();
            for (String s : parts) {
                if (!s.trim().isEmpty())
                    list.add(Integer.parseInt(s.trim()));
            }
            return list.toArray(new Integer[0]);
        } catch (NumberFormatException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("输入提示");
                alert.setHeaderText(null);
                alert.setContentText("格式错误：请输入数字序列");
                alert.showAndWait();
            });
            return new Integer[0];
        }
    }

    /**
     * 静态启动入口，适配 AlgorithmLab 调用
     */
    public static void launch(BaseTree<Integer> algorithm, Integer[] initialData) {
        TreeWindow window = new TreeWindow(algorithm);
        javafx.stage.Stage stage = new javafx.stage.Stage();
        try {
            window.start(stage);

            // 初始数据注入逻辑需在 UI 渲染后进行
            if (initialData != null && initialData.length > 0) {
                Platform.runLater(() -> {
                    String text = Arrays.toString(initialData).replaceAll("[\\[\\]]", "");
                    window.inputField.setText(text);
                    window.setTask(() -> {
                        for (Integer val : initialData)
                            algorithm.put(val);
                    });
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}