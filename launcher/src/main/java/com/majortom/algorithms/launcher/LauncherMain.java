package com.majortom.algorithms.launcher;

import com.majortom.algorithms.App;

/**
 * 统一 Java 启动入口。
 * 运行这个 main 类即可启动整个桌面程序。
 */
public final class LauncherMain {

    private LauncherMain() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
