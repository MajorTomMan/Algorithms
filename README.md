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

各结构直接使用已有 Snapshot 作为保存与恢复契约，构造入口优先使用 `fromSnapshot()`，不再为图或数组重复定义独立的数据模型与初始化器。独立的 `data-io` 模块只负责读取/写入数据格式。详细示例见 [docs/数据初始化与文件读写.md](docs/数据初始化与文件读写.md)。
