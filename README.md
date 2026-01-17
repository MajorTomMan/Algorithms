Algorithms Visualizer 项目文档

项目名称：Algorithms Visualizer

技术标签：

- Java 21
- Maven 3.11.0
- MIT License

项目简介：
这是一个基于 Java 的算法与数据结构可视化工具，通过图形界面直观展示常见算法的执行过程。
项目支持多种经典算法模块，采用模块化设计，核心逻辑与可视化层完全解耦，便于扩展新算法或修改渲染方式。

核心功能：

- 算法步进式可视化，支持暂停、单步执行、重置
- 实时统计：比较次数、操作次数、执行耗时
- 中英文界面切换（基于 ResourceBundle 国际化）
- 统一的后台线程管理，保证计算与渲染同步
- “乱”风格视觉设计，使用红、蓝、黄、紫主色调，融入家纹符号增强模块辨识度

当前支持的模块：

- 排序：冒泡、选择、插入、归并、快速等，支持随机数据生成
- 树：二叉搜索树（BST）的插入、删除、平衡过程
- 图：基于 GraphStream 的无向/有向图，支持 BFS、DFS 遍历
- 迷宫：多种生成算法（BFS、DFS、Prim、Union-Find）与求解算法（A\*、BFS）

技术栈：

- 语言与构建：Java 21 + Maven 3.11.0
- 界面框架：JavaFX + AtlantaFX（深色现代主题）
- 图渲染：GraphStream
- 其他：JUnit 5 测试、ResourceBundle 国际化、单线程算法执行调度

架构设计：

项目采用清晰的分层结构，核心逻辑与界面表现高度分离。

可视化层 (visualization)
├── Controller（交互逻辑、FXML 管理）
│ 示例：MazeController 处理迷宫生成与求解事件
├── Visualizer（渲染层，使用 Canvas 或 GraphStream）
│ 示例：SquareMazeVisualizer 使用 JavaFX GraphicsContext 绘制方格迷宫
└── ThreadManager（算法执行调度与步进同步）

        ↑ 通过 SyncListener 接口进行桥接

核心逻辑层 (core)
├── BaseStructure（数据容器 + 统计）
│ 示例：ArrayMaze 继承 BaseMaze，封装 int[][] 网格数据
├── BaseAlgorithms（算法抽象基类）
│ 示例：BFSMazeGenerator 继承 BaseMazeAlgorithms，实现迷宫生成逻辑
└── 各子模块（basic / sort / tree / graph / maze）
示例：UnionFindMazeGenerator 使用并查集优化迷宫连通性

## 算法执行完整流程（Execution Flow）

以下流程描述一次算法从“用户点击开始”到“界面完成刷新”的完整生命周期：

1. 用户在可视化界面点击 Start / Step
2. Controller 接收事件，调用 ThreadManager 启动或唤醒算法线程
3. AlgorithmThread 在后台线程中执行算法逻辑
4. 算法在关键步骤调用 sync()
5. sync() 触发 SyncListener，将当前 BaseStructure 状态推送至可视化层
6. Visualizer 根据最新数据模型进行绘制
7. ThreadManager 根据当前模式：
   - 单步模式：阻塞等待下一次用户操作
   - 连续模式：sleep 指定延迟后继续执行
8. 算法结束或被 Reset，中断线程并重置数据状态

该流程保证了 **“一次算法操作 → 一次界面刷新”** 的严格对应关系。

---

## 线程模型与并发设计

### 设计原则

- **算法执行始终运行在后台单线程**
- **JavaFX UI 更新始终运行在 FX Application Thread**
- 两者通过监听器与调度器进行解耦协作

### 线程划分

- UI 线程
  - 负责用户交互、FXML 控件更新
  - 不执行任何耗时算法逻辑

- 算法线程（单线程池）
  - 串行执行算法步骤
  - 保证算法执行顺序确定性
  - 避免多线程导致的可视化状态竞争

### 为什么选择单线程算法调度？

- 教学与演示场景优先考虑 **可预测性**
- 算法过程更易被理解与回放
- 避免多线程下“状态跳变”导致的视觉混乱

---

## 项目目录结构说明

```txt
src/main/java
├── com.xxx.algorithms
│ ├── App // 程序入口
│ ├── core // 核心逻辑层
│ │ ├── BaseStructure // 数据结构抽象 + 统计
│ │ ├── BaseAlgorithms // 算法抽象基类
│ │ └── listener // Sync / Step 监听器
│ ├── visualization // 可视化层
│ │ ├── controller // 各模块 Controller
│ │ ├── visualizer // Canvas / Graph 渲染
│ │ └── ThreadManager // 算法执行调度
│ ├── sort / tree / graph
│ └── maze // 各功能模块实现
│
src/main/resources
├── fxml // 各模块界面布局
├── style // UI 样式与主题
├── i18n // 国际化资源文件
└── assets // 图标与纹理
```

目录设计遵循以下原则：

- core 层不依赖 JavaFX
- visualization 层不包含算法逻辑
- 每个模块可独立演化

---

## 设计边界与刻意不做的事情

为了保持系统结构清晰，本项目**刻意不支持**以下特性：

- 多算法并行同时渲染（会破坏教学可读性）
- 高并发或分布式算法执行
- 业务型数据处理或性能基准测试

这些限制是 **设计取舍，而非能力不足**。

---

## 适合人群说明

本项目特别适合以下人群：

- 正在学习算法与数据结构的学生
- 需要进行课堂演示或技术分享的讲师
- 希望理解算法“过程而非结果”的开发者
- 对架构设计与可视化解耦感兴趣的工程师

---

## 未来可扩展方向（规划）

- 算法执行历史回放（时间轴）
- 多算法结果对比（同输入，不同算法）
- 可导出的执行数据（JSON / CSV）
- Web 版本前端（保留 core 层，重写 visualization 层）

---

## 文档定位说明

本项目文档并非 API 手册，而是：

- 架构说明文档
- 扩展指南
- 教学与理解辅助材料

如需查看具体实现细节，请直接参考源码。

核心设计亮点：

1. 统一数据模型
   所有模块数据实体均继承 BaseStructure，提供一致的统计接口与快照功能（copy 方法），支持执行中创建数据副本用于回放或对比。

2. 监听器驱动同步
   算法通过 sync() 方法主动推送状态，SyncListener 将状态传递至可视化层，保证每步操作后立即刷新界面。

3. 线程安全步进控制
   AlgorithmThreadManager 使用单线程池 + Semaphore 实现精确的“计算→渲染→等待”循环，支持动态调整延迟。

4. 模块插件化设计
   每个功能模块拥有独立的 Controller、Visualizer 和 FXML 文件，支持热插拔式扩展。
   国际化通过 I18N.createStringBinding() 动态绑定字符串。

可扩展性说明：

添加同一模块内的新算法：

1. 继承对应模块算法基类（如 BaseSortAlgorithms、BaseMazeAlgorithms）
2. 实现核心执行方法（sort / execute / run）
3. 在对应 Controller 的选择器（ComboBox）中注册新选项

添加全新数据结构与可视化：

1. 创建新的 BaseStructure 实现类，实现 getData、copy、clear 等方法
2. 创建对应的 BaseVisualizer 子类，实现 draw 渲染逻辑
3. （可选）新增模块 Controller 和 FXML 文件

添加全新功能模块：

1. 创建新 Controller（继承 BaseModuleController）
2. 创建对应 Visualizer
3. 在 MainController 的模块切换逻辑中添加分支
4. 使用 Scene Builder 设计对应的 FXML 控制面板

其他扩展方向：

- 增加新语言：新增 language_xx.properties 文件，通过 I18N.setLocale 切换
- 自定义渲染：Visualizer 支持 AnimationTimer 等动态效果，可轻松添加主题或 3D 支持
- 性能优化：GraphStream 已支持复杂图渲染，可进一步引入多线程渲染（需调整 ThreadManager）
- 当前主要局限：依赖 JavaFX，若迁移至 Web 需重写可视化层；单线程池更适合教育场景，不适合高并发

安装与运行：

环境要求：

- JDK 21 或更高（推荐 Oracle JDK / OpenJDK）
- Maven 3.6+（推荐 3.11+）
- 操作系统：Windows / Linux / macOS（需支持图形界面的 JavaFX 环境）

构建与启动步骤：

1. 克隆仓库（请替换为实际地址）
   git clone git@github.com:MajorTomMan/Algorithms.git
   cd Algorithms

2. 编译打包
   mvn clean install

3. 使用 Maven 插件直接运行（推荐方式）
   mvn javafx:run

4. 或通过 IDE 运行：
   打开项目 → 运行主类 com.majortom.algorithms.App
   建议添加 VM 参数：-Dprism.order=sw （优化部分系统渲染）

快速使用指南：

1. 启动后在左侧选择模块（排序 / 树 / 图 / 迷宫）
2. 各模块基本操作：
   排序：调整规模滑块 → 生成数据 → 运行，观察柱状动画
   树：输入数值（支持逗号分隔批量）→ 插入或删除
   图：添加节点/边 → 启动 BFS/DFS，观察高亮遍历
   迷宫：选择生成算法 → 调节密度 → 生成 → 选择求解算法
3. 通用控制按钮：
   Start / Pause / Reset
   右上角 Lang 切换语言
   下方日志区显示实时执行信息
4. 暂停后可手动单步观察细节变化

如何在当前项目框架中扩展新算法或新增可视化实现

项目框架设计高度模块化，支持两种最常见的扩展方式：

1. 在现有模块下增加新的算法实现（不改变可视化风格）
2. 为现有算法增加全新的可视化风格（不修改算法逻辑）

以下分别提供详细、可直接操作的步骤与代码示例。

场景一：在现有模块下扩展新算法
典型案例：在“排序”模块中新增“希尔排序”（Shell Sort）

步骤：

1.  创建算法实现类
    在目录 core/sort/algorithms 下新建类文件 ShellSort.java

```java
    package com.majortom.algorithms.core.sort.algorithms;

    import com.majortom.algorithms.core.sort.BaseSort;
    import com.majortom.algorithms.core.sort.BaseSortAlgorithms;

    public class ShellSort<T extends Comparable<T>> extends BaseSortAlgorithms<T> {

        @Override
        public void sort(BaseSort<T> entity) {
            T[] arr = entity.getData();
            int n = arr.length;

            // 经典希尔增量序列，也可替换为其他序列
            for (int gap = n / 2; gap > 0; gap /= 2) {
                for (int i = gap; i < n; i++) {
                    T temp = arr[i];
                    int j = i;

                    // 插入排序的内层循环
                    while (j >= gap && compare(arr[j - gap], temp) > 0) {
                        entity.incrementCompare();           // 统计比较次数
                        arr[j] = arr[j - gap];
                        entity.incrementAction();            // 统计移动/赋值次数
                        sync(entity, j, j - gap);            // 关键：触发可视化更新
                        j -= gap;
                    }

                    if (j != i) {
                        arr[j] = temp;
                        entity.incrementAction();
                        sync(entity, j, i);                  // 同步最终插入位置
                    }
                }
            }
        }

    }
```

    重要提醒：所有关键比较与数据移动处必须调用 sync() 方法，否则可视化不会更新。

2.  在 SortController 中注册新算法
    修改 SortController.java，在初始化算法选择器时添加选项：

    ```java
    // 在 initialize() 方法或构造阶段
    algoSelector.getItems().addAll(
    "Bubble Sort",
    "Merge Sort",
    "Quick Sort",
    "Shell Sort" // 新增项
    );

    // 在执行算法的逻辑中（例如 handleSort() 或 startAlgorithm() 内）
    String selected = algoSelector.getValue();
    if ("Shell Sort".equals(selected)) {
    currentAlgorithm = new ShellSort<>();
    }
    ```

3.  测试验证
    启动程序 → 进入排序模块 → 算法下拉选择 "Shell Sort"
    → 设置规模（如 100）→ 生成随机数组 → 点击 Run
    观察柱状图是否正确呈现分组插入的过程（分组内小范围排序，逐渐扩大）

场景二：为现有算法新增全新可视化风格
典型案例：为排序模块新增“散点轨迹线”风格（类似粒子运动轨迹可视化）

步骤：

1.  创建新的 Visualizer 实现类
    在目录 visualization/impl/visualizer 下新建 ParticleTrajectoryVisualizer.java

    ```java
    package com.majortom.algorithms.visualization.impl.visualizer;

    import com.majortom.algorithms.core.sort.BaseSort;
    import com.majortom.algorithms.visualization.base.BaseSortVisualizer;
    import javafx.scene.canvas.GraphicsContext;
    import javafx.scene.paint.Color;

    public class ParticleTrajectoryVisualizer<T extends Comparable<T>> extends BaseSortVisualizer<T> {

        @Override
        protected void drawSortContent(BaseSort<T> data, Object a, Object b) {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

            T[] arr = data.getData();
            double barWidth = canvas.getWidth() / arr.length;
            double maxHeight = canvas.getHeight() * 0.8;

            // 绘制所有数据点的淡化轨迹（历史残影效果）
            gc.setGlobalAlpha(0.3);
            for (int i = 0; i < arr.length; i++) {
                double x = i * barWidth + barWidth / 2;
                double value = Double.parseDouble(arr[i].toString());
                double y = maxHeight - (value * (maxHeight / 100));

                gc.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.4));
                gc.fillOval(x - 4, y - 4, 8, 8);
            }

            // 绘制当前比较或交换的轨迹线（高亮）
            if (a instanceof Integer idxA && b instanceof Integer idxB) {
                double x1 = idxA * barWidth + barWidth / 2;
                double y1 = maxHeight - Double.parseDouble(arr[idxA].toString()) * (maxHeight / 100);
                double x2 = idxB * barWidth + barWidth / 2;
                double y2 = maxHeight - Double.parseDouble(arr[idxB].toString()) * (maxHeight / 100);

                gc.setGlobalAlpha(1.0);
                gc.setStroke(Color.ORANGERED);
                gc.setLineWidth(2.5);
                gc.strokeLine(x1, y1, x2, y2);
            }
        }

    }
    ```

2.  在 SortController 中增加风格切换支持
    添加一个 ComboBox 用于选择可视化风格：

    ```java
    // 新增控件
    ComboBox<String> visualStyleSelector = new ComboBox<>();
    visualStyleSelector.getItems().addAll("Histogram (Default)", "Particle Trajectory");
    visualStyleSelector.getSelectionModel().selectFirst();

    // 在渲染时根据选择动态创建 visualizer
    String style = visualStyleSelector.getValue();
    if ("Particle Trajectory".equals(style)) {
    visualizer = new ParticleTrajectoryVisualizer<>();
    } else {
    visualizer = new HistogramSortVisualizer<>();
    }

    // 每次数据变化或算法步进时调用
    visualizer.render(sortData, focusA, focusB);
    ```

3.  测试验证
    启动排序模块 → 选择任意排序算法（如冒泡）
    → 将可视化风格切换至 "Particle Trajectory"
    → 运行算法 → 观察散点轨迹 + 交换时橙红色连接线的动态效果

通用扩展建议（适用于所有情况）

- 核心原则：所有关键操作点必须调用 sync(entity, a, b) 以触发可视化刷新
- 复用现有资源：新可视化应尽量调用 BaseVisualizer 中的通用方法（如颜色获取、家纹绘制）
- 测试覆盖：扩展完成后立即编写单元测试，验证统计计数（compareCount/actionCount）准确
- 模块预留扩展点：若未来计划支持更多模块，可在 MainController 中使用 Map<String, Supplier<BaseModuleController>> 实现动态注册

贡献规范：

欢迎提交 Pull Request 或 Issue

- 新算法建议附带单元测试（src/test 下，使用 JUnit）
- 请尽量保持现有“乱”风格视觉统一性（参考 style/ui_theme.css）
- 重大改动或新增模块建议先开 Issue 讨论
- 代码风格遵循 Java 规范，建议使用 IntelliJ 默认格式化
- 提交前请运行 mvn test 确保测试全部通过

许可证：
MIT License
详细内容请查看项目根目录 LICENSE 文件

感谢您的关注与支持。
