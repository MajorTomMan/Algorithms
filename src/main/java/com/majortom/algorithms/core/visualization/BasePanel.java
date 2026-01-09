package com.majortom.algorithms.core.visualization;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * 通用可视化画布基类（Abstract Base Class for Visualization Panels）
 * * 职责：
 * 1. 提供算法可视化所需的绘图容器。
 * 2. 维护待渲染的数据模型及算法运行时的视觉焦点。
 * 3. 规范子类的渲染生命周期（计算比例尺 -> 执行渲染）。
 * * @param <T> 数据模型类型（例如：int[] 用于排序算法，Node 用于树结构等）
 */
public abstract class BasePanel<T> extends JPanel {
    /** 核心数据状态：存储当前需要被可视化的算法数据结构 */
    protected T data;

    /** 视觉焦点 A：用于标记算法当前操作的元素（如排序中的“左指针”或“父节点”） */
    protected Object activeA;

    /** 视觉焦点 B：用于标记算法当前操作的对比元素（如排序中的“右指针”或“子节点”） */
    protected Object activeB;

    /** 绘图单元尺寸：根据数据规模和窗口大小动态计算的步长或单位 */
    protected int cellSize;

    /** 绘图边距：防止图形元素贴合窗口边界 */
    protected final int padding = 20;

    /**
     * 构造函数
     * 
     * @param data 初始化的数据模型
     */
    public BasePanel(T data) {
        this.data = data;
        // 设置默认背景色为深色（IDE 风格风格）
        setBackground(new Color(33, 37, 43));
    }

    /**
     * 数据状态更新接口
     * 外部控制器（如 BaseFrame 或算法引擎）通过此方法同步算法最新状态并触发重绘。
     * * @param data 最新的数据模型
     * 
     * @param a 当前活跃点 A
     * @param b 当前活跃点 B
     */
    public void updateData(T data, Object a, Object b) {
        this.data = data;
        this.activeA = a;
        this.activeB = b;
        // 调用 Swing 体系的重绘方法，触发 paintComponent 的执行
        this.repaint();
    }

    /**
     * 重写 Swing 绘图管线方法
     * 统筹绘图的前置处理与具体渲染逻辑。
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 数据模型为空时跳过渲染逻辑，防止运行时异常
        if (data == null)
            return;

        Graphics2D g2d = (Graphics2D) g;

        // 渲染设置：开启抗锯齿，提升图形边缘的平滑度
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 步骤 1：调用子类实现的逻辑，计算当前窗口尺寸下的最佳比例尺
        calculateScale();

        // 步骤 2：调用子类实现的具体映射逻辑，将数据转化为几何图形
        render(g2d);
    }

    /**
     * 抽象渲染方法
     * 需由子类实现：定义具体的图形绘制逻辑（如绘制柱状图、二叉树节点、图连接等）。
     * * @param g 已经过初始化配置的 Graphics2D 上下文
     */
    protected abstract void render(Graphics2D g);

    /**
     * 抽象比例计算方法
     * 需由子类实现：根据 data 数组长度、树的高度等维度，结合 getWidth() 和 getHeight() 计算 cellSize。
     */
    protected abstract void calculateScale();
}