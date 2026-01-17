package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import java.util.Arrays;

/**
 * 排序视觉呈现基类
 * 职责：
 * 1. 提供数组高度到画布坐标的缩放映射逻辑。
 * 2. 统一排序柱状图的布局规范（留白、间距）。
 * 3. 继承父类《乱》配色体系，保持 UI 风格一致性。
 */
public abstract class BaseSortVisualizer<T extends Comparable<T>> extends BaseVisualizer<BaseSort<T>> {

    @Override
    protected void draw(BaseSort<T> sortData, Object a, Object b) {
        T[] data = sortData.getData();
        if (data == null || data.length == 0)
            return;

        // 1. 获取度量基准
        double maxVal = getMaxValue(data);

        // 2. 计算布局参数
        double paddingSide = 40.0;
        double paddingBottom = 70.0; // 为下方数值预留空间
        double paddingTop = 40.0; // 顶部留白

        double availableW = canvas.getWidth() - 2 * paddingSide;
        double availableH = canvas.getHeight() - paddingBottom - paddingTop;

        // 3. 计算缩放因子 (Scale Factor)
        double scale = maxVal <= 0 ? 1 : availableH / maxVal;
        double barW = availableW / data.length;

        // 4. 执行具体渲染 (交给子类如 HistogramSortVisualizer 实现)
        drawSortContent(sortData, a, b, barW, scale, paddingSide, paddingBottom);
    }

    /**
     * 子类实现具体的绘制形态
     * 
     * @param barW         单个柱体的最大宽度（含间距）
     * @param scale        垂直缩放比例系数
     * @param offsetLeft   左侧起始偏移量
     * @param offsetBottom 底部起始偏移量
     */
    protected abstract void drawSortContent(BaseSort<T> sortData, Object a, Object b,
            double barW, double scale,
            double offsetLeft, double offsetBottom);

    /**
     * 高性能最大值获取逻辑
     */
    protected double getMaxValue(T[] data) {
        return Arrays.stream(data)
                .mapToDouble(item -> {
                    try {
                        return Double.parseDouble(item.toString());
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .max()
                .orElse(1.0);
    }

    /**
     * 通用柱体绘制工具
     */
    protected void drawBar(double x, double y, double w, double h, javafx.scene.paint.Color color) {
        double gap = w > 4 ? 1.5 : 0.2; // 窄柱体自动减小间距
        gc.setFill(color);
        gc.fillRect(x, y, Math.max(0.5, w - gap), h);
    }
}