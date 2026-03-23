package com.majortom.algorithms.utils;

import java.util.List;

import com.majortom.algorithms.core.graph.node.Vertex;

public class GraphUtils {
    /**
     * 将所有顶点按圆形均匀分布在画布中心
     */
    public static <V> void autoLayout(List<Vertex<V>> vertices, int width, int height) {
        if (vertices == null || vertices.isEmpty())
            return;

        int count = vertices.size();

        // 这里的 padding 改为 80 或者更大，确保节点不会贴着窗体边缘
        int padding = 80;
        int radius = Math.min(width, height) / 2 - padding;

        int centerX = width / 2;
        int centerY = height / 2;

        for (int i = 0; i < count; i++) {
            // - Math.PI / 2 让第一个点从 12 点钟方向开始
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            int x = (int) (centerX + radius * Math.cos(angle));
            int y = (int) (centerY + radius * Math.sin(angle));

            // 增加一个简单的越界保护（可选）
            x = Math.max(padding, Math.min(width - padding, x));
            y = Math.max(padding, Math.min(height - padding, y));

            vertices.get(i).setPosition(x, y);
        }
    }
}
