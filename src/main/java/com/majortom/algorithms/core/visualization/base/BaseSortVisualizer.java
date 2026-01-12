package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;

/**
 * 排序视觉呈现基类
 * 职责：定义排序通用的坐标缩放逻辑与颜色常数。
 * 
 * @param <T> 数据类型
 */
public abstract class BaseSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    // --- 排序专用霓虹配色 ---
    protected static final Color COLOR_BAR_BASE = Color.rgb(207, 216, 220, 0.9); // CRYSTAL_WHITE 基调
    protected static final Color COLOR_ACTIVE = Color.rgb(126, 87, 194); // START_VIOLET (主焦点)
    protected static final Color COLOR_COMPARE = Color.rgb(0, 160, 255); // NEON_BLUE (副焦点)
    protected static final Color COLOR_FINISHED = Color.rgb(0, 255, 150); // 结束时的翠绿色

    @Override
    protected void draw(BaseSort<T> sortData, Object a, Object b) {
        clear();
        if (sortData == null || sortData.getData() == null || sortData.size() == 0)
            return;

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
        double max = 0.001; // 防止除以0
        for (T item : data) {
            try {
                double val = Double.parseDouble(item.toString());
                if (val > max)
                    max = val;
            } catch (Exception e) {
                max = 100.0;
            }
        }
        return max;
    }
}