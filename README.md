# Algorithms Lab

一个基于 Java 21 的算法可视化与实验仓库。

现在项目已经整理成多模块结构：

- `core`：算法核心、数据结构、执行上下文、快照与导出模型
- `visualization`：JavaFX 可视化主程序
- `launcher`：统一 Java 启动入口
- `sandbox`：练习代码、LeetCode/算法实验样例

## 快速开始

在项目根目录直接启动可视化程序：

```bash
mvn -f launcher/pom.xml exec:java
```

如果想从 IDE 里直接启动，运行下面这个主类即可：

```txt
com.majortom.algorithms.launcher.LauncherMain
```

常用 Maven 命令：

```bash
mvn test
mvn -DskipTests package
mvn -pl sandbox -am test
mvn -f launcher/pom.xml exec:java
```

## 环境要求

- JDK 21+
- Maven 3.9+ 或兼容版本
- 支持 JavaFX 的桌面环境

## 目录结构

```txt
.
├── core/
│   └── src/main/java/com/majortom/algorithms/
│       ├── core/
│       └── utils/AlgorithmsUtils.java
├── visualization/
│   ├── src/main/java/com/majortom/algorithms/
│   │   ├── App.java
│   │   ├── utils/EffectUtils.java
│   │   └── visualization/
│   └── src/main/resources/
│       ├── fxml/
│       ├── language/
│       ├── style/
│       └── data/
├── launcher/
│   └── src/main/java/com/majortom/algorithms/launcher/
├── sandbox/
│   └── src/main/java/com/majortom/algorithms/app/
└── pom.xml
```

## 模块说明

### `core`

负责纯逻辑层，不依赖 JavaFX。

主要内容：

- 基础数据结构与算法抽象
- 排序、树、图、迷宫等核心模型
- `ExecutionContext` 执行上下文
- `ExecutionTimeline` / `ExecutionRecord` 回放记录
- 快照序列化与导出模型

适合做：

- 新增算法
- 扩展数据结构
- 调整执行记录与快照机制

### `visualization`

负责 GUI、交互、回放、导出入口与模块注册。

主要内容：

- JavaFX 主界面
- 各模块控制器和渲染器
- 模块注册表
- 国际化资源
- 根程序入口

适合做：

- 新增界面模块
- 调整交互流程
- 修改样式与 FXML

### `launcher`

负责统一 Java 启动入口。

主要内容：

- `LauncherMain`
- 根目录 Maven 启动命令的落点
- IDE 运行入口

适合做：

- 调整桌面程序启动参数
- 增加未来的 CLI 或启动模式切换

### `sandbox`

负责练习代码和独立实验代码，不属于主程序运行主干。

它依赖 `core`，并通过 Maven 正常引入 `algs4`，不再使用 `systemPath`。

## 当前能力

主程序当前支持的方向：

- 排序可视化
- 树结构可视化
- 图遍历可视化
- 迷宫生成与求解可视化
- 执行时间轴回放
- 同输入结果比较
- 执行记录导出为 JSON

## 执行模型

项目现在使用统一执行上下文驱动算法运行。

一次算法运行大致流程如下：

1. 控制器创建 `ExecutionContext`
2. 算法在关键步骤调用 `sync(...)`
3. `ExecutionContext` 生成结构快照并写入时间轴
4. UI 渲染当前帧
5. 运行完成后生成 `ExecutionRecord`
6. 记录可用于回放、比较和导出

这套模型把以下能力收敛到了同一个运行时对象里：

- sync
- cancel
- delay
- stats
- timeline
- messages

## 国际化与文案约定

资源文件位于：

- [visualization/src/main/resources/language/language_zh.properties](/home/hujunhao/Disk/hu_folder/Projects/algorithms/visualization/src/main/resources/language/language_zh.properties)
- [visualization/src/main/resources/language/language_en.properties](/home/hujunhao/Disk/hu_folder/Projects/algorithms/visualization/src/main/resources/language/language_en.properties)

当前 key 命名约定：

- `label.*`：标签文本
- `prompt.*`：输入提示
- `action.*`：按钮或动作文本
- `message.*`：日志消息
- `stats.*`：统计展示
- `module.*`：模块名称
- `algorithm.*`：算法名称

## 开发建议

### 新增一个算法

1. 在 `core` 中实现算法类
2. 让算法通过统一执行上下文产生快照
3. 在 `visualization` 中接入控制器或选择器
4. 补充 `language_zh.properties` / `language_en.properties`

### 新增一个可视化模块

1. 在 `core` 中定义数据模型和算法
2. 在 `visualization` 中增加 Controller、Visualizer、FXML
3. 在 `ModuleRegistry` 中注册模块

### 导出执行记录

运行一次算法后，可以通过主界面的导出按钮，或后续扩展 CLI 方式，生成 JSON 文件到：

```txt
exports/
```

## 已知情况

- 目前自动化测试仍然偏少，构建能通过，但回归网还不够厚
- `sandbox` 中是实验代码集合，风格和质量不完全统一
- `core` 里仍有部分历史算法类未完全实现，但它们不影响当前主程序主链路

## 推荐入口

如果你的目标是：

- 使用程序：运行 `mvn -f launcher/pom.xml exec:java`
- 在 IDE 中启动：运行 `com.majortom.algorithms.launcher.LauncherMain`
- 看框架：从 `core` 和 `visualization` 开始
- 写练习代码：进入 `sandbox`
