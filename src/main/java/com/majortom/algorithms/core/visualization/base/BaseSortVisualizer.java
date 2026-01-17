package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;

/**
 * 排序视觉呈现基类
 * 统一定义黑泽明《乱》配色体系下的核心色值。
 */
public abstract class BaseSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    // --- 《乱》中立配色方案 ---
    protected final Color RAN_RED = Color.rgb(180, 0, 0); // 常规：红色 (太郎)
    protected final Color RAN_BLUE = Color.rgb(0, 120, 255); // 活跃：蓝色 (次郎)
    protected final Color RAN_GOLD = Color.rgb(220, 180, 0); // 比较：黄色 (三郎)
    protected final Color BONE_WHITE = Color.rgb(240, 240, 230); // 突发：骨白 (意志)

    @Override
    protected void draw(BaseSort<T> sortData, Object a, Object b) {
        clear();
        if (sortData == null || sortData.getData() == null || sortData.size() == 0) {
            return;
        }
        drawSortContent(sortData, a, b);
    }

    /**
     * 子类实现具体的绘制形态
     */
    protected abstract void drawSortContent(BaseSort<T> sortData, Object a, Object b);

    /**
     * 通用辅助方法：获取数组中的最大值，用于高度缩放
     */
    protected double getMaxValue(T[] data) {
        double max = 0.001;
        for (T item : data) {
            try {
                double val = Double.parseDouble(item.toString());
                if (val > max)
                    max = val;
            } catch (Exception e) {
                // 忽略非数字
            }
        }
        return max;
    }
}