package com.majortom.algorithms;

import com.majortom.algorithms.visualization.launcher.AlgorithmVisualizerLauncher;

/**
 * visualization 模块的应用入口。
 *
 * <p>实际 JavaFX 启动逻辑放在 {@link AlgorithmVisualizerLauncher} 中。
 * 这个类保留一个简短 main，方便 Maven、IDE 或 launcher 模块统一调用。</p>
 */
public class App {
    /**
     * 启动算法可视化桌面程序。
     *
     * @param args 命令行参数，会透传给 JavaFX Application
     */
    public static void main(String[] args) {
        AlgorithmVisualizerLauncher.launch(args);
    }
}
