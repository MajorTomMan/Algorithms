package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;

import javafx.animation.AnimationTimer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 树算法可视化基类。
 *
 * <p>它维护节点当前位置和目标位置，实现树结构变化时的平滑过渡。
 * 子类只需要计算布局和递归绘制节点，动画计时器会持续把当前位置插值到目标位置。</p>
 *
 * @param <T> 树节点数据类型
 */
public abstract class BaseTreeVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseTree<T>> {

    /**
     * 位置插值系数，越大节点移动越快。
     */
    protected static final double LERP_FACTOR = 0.18;

    /**
     * 当前屏幕位置映射。
     */
    protected final Map<TreeNode<T>, Point> currentPosMap = new ConcurrentHashMap<>();

    /**
     * 目标屏幕位置映射。
     */
    protected final Map<TreeNode<T>, Point> targetPosMap = new ConcurrentHashMap<>();

    /**
     * 最近一次渲染的树实例。
     */
    protected BaseTree<T> treeInstance;

    /**
     * 当前焦点对象。
     */
    protected Object focusA;
    protected Object focusB;

    /**
     * 自动缩放比例。
     */
    protected double autoScale = 1.0;

    /**
     * 自动布局横向偏移。
     */
    protected double autoOffsetX = 0;

    /**
     * 自动布局纵向偏移。
     */
    protected double autoOffsetY = 80;
    private final AnimationTimer frameTimer;
    private boolean animationRunning;
    private boolean treeAnimationRequested = true;

    /**
     * 创建树可视化器并启动动画计时器。
     */
    public BaseTreeVisualizer() {
        // 启动统一的动画计时器
        this.frameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimationPositions();
                renderFrame();
            }
        };
        resumeTreeAnimation();
    }

    /**
     * 接收树快照并触发布局更新。
     *
     * @param tree 树快照
     * @param a 当前焦点节点
     * @param b 第二焦点节点
     */
    @Override
    protected void draw(BaseTree<T> tree, Object a, Object b) {
        if (tree == null || tree.getRoot() == null) {
            this.treeInstance = null;
            return;
        }
        this.treeInstance = tree;
        this.focusA = a;
        this.focusB = b;

        // 触发布局计算（由子类实现具体算法：如递归排列、分层排列）
        updateLayout(tree);
    }

    /**
     * 核心渲染流程：清屏、应用变换、递归绘制。
     */
    private void renderFrame() {
        // 1. 无论有没有数据，每一帧都先清屏
        clear();
        if (treeInstance == null || treeInstance.getRoot() == null)
            return;
        gc.save();
        gc.translate(autoOffsetX, autoOffsetY);
        gc.scale(autoScale, autoScale);

        renderTreeRecursive(treeInstance.getRoot());

        gc.restore();
        drawTransientFeedbackOverlay();
    }

    /**
     * 子类计算节点目标布局。
     *
     * @param tree 当前树快照
     */
    protected abstract void updateLayout(BaseTree<T> tree);

    /**
     * 子类递归绘制树节点。
     *
     * @param node 当前节点
     */
    protected abstract void renderTreeRecursive(TreeNode<T> node);

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        VisualizationActionType actionType = event.actionType();
        switch (actionType) {
            case EXECUTION_PAUSE -> setTreeAnimationRequested(false);
            case EXECUTION_RESUME, EXECUTION_START, TREE_INSERT, TREE_DELETE, TREE_RANDOM -> setTreeAnimationRequested(true);
            case EXECUTION_RESET -> resetTreeVisualizationState();
            default -> {
            }
        }
    }

    @Override
    public void onVisualizationReset() {
        resetTreeVisualizationState();
        clear();
    }

    @Override
    public void onModuleAttached(String moduleId) {
        setTreeAnimationRequested(true);
    }

    @Override
    public void onModuleDetached(String moduleId) {
        setTreeAnimationRequested(false);
        resetTreeVisualizationState();
        clear();
    }

    @Override
    protected void onResizeStateChanged(boolean resizing) {
        if (resizing) {
            pauseTreeAnimation();
        } else if (treeAnimationRequested) {
            resumeTreeAnimation();
        }
    }

    protected final void resumeTreeAnimation() {
        if (!animationRunning) {
            frameTimer.start();
            animationRunning = true;
        }
    }

    protected final void pauseTreeAnimation() {
        if (animationRunning) {
            frameTimer.stop();
            animationRunning = false;
        }
    }

    private void setTreeAnimationRequested(boolean requested) {
        treeAnimationRequested = requested;
        if (requested) {
            if (!isResizeInProgress()) {
                resumeTreeAnimation();
            }
        } else {
            pauseTreeAnimation();
        }
    }

    protected void resetTreeVisualizationState() {
        treeInstance = null;
        focusA = null;
        focusB = null;
        currentPosMap.clear();
        targetPosMap.clear();
        autoScale = 1.0;
        autoOffsetX = 0;
        autoOffsetY = 80;
    }

    /**
     * 更新节点位置，实现平滑移动。
     */
    private void updateAnimationPositions() {
        targetPosMap.forEach((node, target) -> {
            Point current = currentPosMap.getOrDefault(node, target);
            currentPosMap.put(node, new Point(
                    current.x + (target.x - current.x) * LERP_FACTOR,
                    current.y + (target.y - current.y) * LERP_FACTOR));
        });
        currentPosMap.keySet().removeIf(node -> !targetPosMap.containsKey(node));
    }

    /**
     * 二维坐标点。
     */
    protected static class Point {
        /**
         * X 坐标。
         */
        public double x, y;

        /**
         * 创建坐标点。
         *
         * @param x X 坐标
         * @param y Y 坐标
         */
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
