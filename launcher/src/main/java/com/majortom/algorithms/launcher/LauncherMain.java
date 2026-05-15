package com.majortom.algorithms.launcher;

import com.majortom.algorithms.App;

/**
 * 统一 Java 启动入口。
 *
 * <p>IDE 或根模块 Maven 命令可以运行这个 main 类，它再委托给 visualization 模块的
 * {@link App#main(String[])}，从而避免用户记忆 JavaFX 具体入口类。</p>
 */
public final class LauncherMain {

    /**
     * 工具入口类不允许实例化。
     */
    private LauncherMain() {
    }

    /**
     * 启动桌面程序。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        App.main(args);
    }
}
