
package com.majortom.algorithms.core.visualization;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

/**
 * 通用可视化画布基类
 * 丢弃 JPanel，使用 JavaFX Region 作为容器，Canvas 作为绘图组件
 * * @param <T> 可视化数据的类型
 */
public abstract class BaseVisualizer<T> extends Region {

    // 响应式属性：数据源
    protected final ObjectProperty<T> data = new SimpleObjectProperty<>();
    // 响应式属性：当前活跃/选中的元素（例如排序中正在比较的两个下标）
    protected final ObjectProperty<Object> activeA = new SimpleObjectProperty<>();
    protected final ObjectProperty<Object> activeB = new SimpleObjectProperty<>();

    // 绘图核心组件
    protected final Canvas canvas = new Canvas();

    // 基础配置
    protected double cellSize;
    protected final double padding = 20.0;

    /**
     * 构造函数
     * 
     * @param initialData 初始数据
     */
    public BaseVisualizer(T initialData) {
        // 1. 初始化数据
        this.data.set(initialData);

        // 2. 将 Canvas 添加到 Region 的子节点列表中
        getChildren().add(canvas);

        // 3. 建立画布与容器的大小绑定：画布随容器自动缩放
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // 4. 注册监听器：当尺寸、数据或状态改变时，自动触发重绘
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> handleRedraw());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> handleRedraw());
        data.addListener((obs, oldVal, newVal) -> handleRedraw());
        activeA.addListener((obs, oldVal, newVal) -> handleRedraw());
        activeB.addListener((obs, oldVal, newVal) -> handleRedraw());

        // 5. 设置初始重绘
        handleRedraw();
    }

    /**
     * 更新数据和活跃状态的统一入口
     * 调用此方法会自动通过属性监听器触发 handleRedraw()
     */
    public void updateState(T newData, Object a, Object b) {
        this.data.set(newData);
        this.activeA.set(a);
        this.activeB.set(b);
    }

    /**
     * 重绘调度逻辑
     */
    private void handleRedraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // 过滤无效的绘图请求
        if (width <= 0 || height <= 0 || data.get() == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // 1. 清除当前画布内容（等同于 Swing 的 super.paintComponent）
        gc.clearRect(0, 0, width, height);

        // 2. 计算缩放比例或布局参数
        onMeasure(width, height);

        // 3. 执行具体的绘图渲染
        draw(gc, width, height);
    }

    /**
     * 子类实现：计算绘图所需的比例、单元格大小等坐标信息
     * 
     * @param width  当前画布宽度
     * @param height 当前画布高度
     */
    protected abstract void onMeasure(double width, double height);

    /**
     * 子类实现：具体的绘图指令
     * 
     * @param gc     JavaFX 绘图上下文 (替代了 Graphics2D)
     * @param width  画布宽度
     * @param height 画布高度
     */
    protected abstract void draw(GraphicsContext gc, double width, double height);

    // --- Getter & Setter ---

    public T getData() {
        return data.get();
    }

    public void setData(T data) {
        this.data.set(data);
    }

    public Object getActiveA() {
        return activeA.get();
    }

    public void setActiveA(Object a) {
        this.activeA.set(a);
    }

    public Object getActiveB() {
        return activeB.get();
    }

    public void setActiveB(Object b) {
        this.activeB.set(b);
    }
}