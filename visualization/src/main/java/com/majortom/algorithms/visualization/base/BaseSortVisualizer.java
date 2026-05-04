package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;

import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * 排序算法可视化基类
 * 职责：计算数据比例、维护颜色状态机、定义排序渲染生命周期
 */
public abstract class BaseSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    private boolean ambientAnimationRequested = true;

    @Override
    protected void draw(BaseSort<T> data, Object a, Object b) {
        clear();
        if (data == null || data.getData() == null)
            return;
        drawSortContent(data, a, b);
        drawTransientFeedbackOverlay();
    }

    /**
     * 子类实现具体的绘制形态（柱状图、点图等）
     */
    protected abstract void drawSortContent(BaseSort<T> sortData, Object a, Object b);

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        VisualizationActionType actionType = event.actionType();
        switch (actionType) {
            case EXECUTION_PAUSE -> setAmbientAnimationRequested(false);
            case EXECUTION_RESUME, EXECUTION_START, SORT_GENERATE, SORT_RUN -> setAmbientAnimationRequested(true);
            case EXECUTION_RESET -> resetSortVisualizationState();
            default -> {
            }
        }
    }

    @Override
    public void onVisualizationReset() {
        resetSortVisualizationState();
        clear();
    }

    @Override
    public void onModuleAttached(String moduleId) {
        setAmbientAnimationRequested(true);
    }

    @Override
    public void onModuleDetached(String moduleId) {
        setAmbientAnimationRequested(false);
        resetSortVisualizationState();
        clear();
    }

    @Override
    protected void onResizeStateChanged(boolean resizing) {
        if (resizing) {
            pauseAmbientAnimation();
        } else if (ambientAnimationRequested) {
            resumeAmbientAnimation();
        }
    }

    private void setAmbientAnimationRequested(boolean requested) {
        ambientAnimationRequested = requested;
        if (requested) {
            if (!isResizeInProgress()) {
                resumeAmbientAnimation();
            }
        } else {
            pauseAmbientAnimation();
        }
    }

    /**
     * 恢复排序可视化中的环境动画。
     * 默认留空，由存在动画的子类按需覆写。
     */
    protected void resumeAmbientAnimation() {
    }

    /**
     * 暂停排序可视化中的环境动画。
     * 默认留空，由存在动画的子类按需覆写。
     */
    protected void pauseAmbientAnimation() {
    }

    /**
     * 清理排序可视化的瞬时状态，如缓存的聚焦索引或动画相位。
     * 默认留空，由具体子类按需覆写。
     */
    protected void resetSortVisualizationState() {
    }

    /**
     * 通用色彩决策引擎 - 适配《乱》配色体系
     * 
     * @param index    当前遍历到的索引
     * @param sortData 排序数据模型
     * @param a        外部传入的比较参数A
     * @param b        外部传入的比较参数B
     */
    protected Color getRanColor(int index, BaseSort<T> sortData, Object a, Object b) {
        // 1. 活跃状态（读写指针）：次郎蓝
        if (index == sortData.getActiveIndex()) {
            return RAN_BLUE;
        }
        // 2. 比较/交互状态：三郎黄（原代码中的 GOLD）
        if (Objects.equals(index, a) || Objects.equals(index, b)) {
            return RAN_YELLOW;
        }
        // 3. 默认状态：太郎红
        return RAN_RED;
    }

    /**
     * 获取数据中的最大值，用于归一化高度计算
     */
    protected double getMaxValue(T[] data) {
        double max = 0;
        for (T item : data) {
            if (item == null)
                continue;
            try {
                double val = Double.parseDouble(item.toString());
                if (val > max)
                    max = val;
            } catch (NumberFormatException e) {
                // 忽略非数值类型
            }
        }
        return max;
    }
}
