package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.visualization.BaseVisualizer;

import javafx.scene.paint.Color;

import java.util.Objects;

/**
 * 排序算法可视化基类。
 *
 * <p>它接收 {@link BaseSort} 快照，负责清屏、空数据保护和排序通用颜色决策。
 * 具体柱状图或点图形态由子类在 {@link #drawSortContent(BaseSort, Object, Object)} 中实现。</p>
 *
 * @param <T> 排序元素类型
 */
public abstract class BaseSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    /**
     * 绘制排序结构。
     *
     * @param data 排序结构快照
     * @param a 第一个焦点索引
     * @param b 第二个焦点索引
     */
    @Override
    protected void draw(BaseSort<T> data, Object a, Object b) {
        clear();
        if (data == null || data.getData() == null)
            return;
        drawSortContent(data, a, b);
    }

    /**
     * 子类实现具体绘制形态。
     *
     * @param sortData 排序结构快照
     * @param a 第一个焦点索引
     * @param b 第二个焦点索引
     */
    protected abstract void drawSortContent(BaseSort<T> sortData, Object a, Object b);

    /**
     * 排序状态颜色决策。
     * 
     * @param index    当前遍历到的索引
     * @param sortData 排序数据模型
     * @param a        外部传入的比较参数A
     * @param b        外部传入的比较参数B
     * @return 当前索引对应的颜色
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
     * 获取数据中的最大值，用于归一化高度计算。
     *
     * @param data 排序数组
     * @return 可解析出的最大数值
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
