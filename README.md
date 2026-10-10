# Algorithms

基于 JavaFX 的数据结构与算法可视化桌面程序，支持结构操作、算法执行与动画回放。

## 启动

环境：**JDK 21、Maven、Python 3.10+**，并确保 `java`、`mvn`、`python` 可在终端使用。

在项目根目录执行：

```bash
python start.py
```

脚本会安装客户端所需的 Maven 模块（跳过测试），然后启动 JavaFX 程序。

## 技术栈

- **语言与构建：** Java 21、Maven；Python 启动脚本
- **桌面 UI：** JavaFX 21、AtlantaFX、FXML / CSS
- **可视化：** GestureFX（视口缩放）、Eclipse ELK（图与树布局）
- **数据与分析：** Jackson、JFR；JOL 为可选内存分析依赖

## 数据读写

独立 Maven 模块 `data-io` 提供 `DataReader<T>`、`DataWriter<T>` 及 JSON 编解码器，不依赖数据结构、算法或 JavaFX。文件读取和安全写入示例见 [data-io/README.md](data-io/README.md)。

## 数据初始化与文件读写

`structures` 模块定义与文件格式无关的 `StructureInitializer<S, D>`，目前提供 `GraphInitializer<T>`、`ArrayInitializer<T>`。图的 `GraphData<T>` 仅包含方向、顶点与带权连接，不保存运行时 ID。

`client` 的 `StructureDataFiles` 把独立 `data-io` 和结构初始化器组合起来，支持图、数组的 JSON 读写。示例和边界说明参见 [docs/数据初始化与文件读写.md](docs/数据初始化与文件读写.md)。
