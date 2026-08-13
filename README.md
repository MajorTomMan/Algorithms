# Algorithms Lab

基于 Java 21 的算法实验与可视化项目。项目将算法协议、算法实现、桌面展示和服务端入口分开组织，算法通过结构化事件与展示层解耦。

## 项目结构

```text
algorithms/
├── core/        公共 API、执行上下文、事件流、EventReducer 和执行统计
├── algorithms/  排序、树、迷宫、图等算法实现及 ProviderCatalog
├── client/      JavaFX 桌面客户端、动画、时间轴和回放
├── server/      Spring Boot 服务端入口
├── other/       练习代码和独立实验
└── docs/        详细架构文档
```

主要依赖方向：

```text
client ──→ algorithms ──→ core
other  ──→ algorithms ──→ core
server ──→ core
```

`client` 和 `server` 互不依赖；`core` 不包含 JavaFX 或 Spring Boot 实现。

## 架构设计

算法通过 `Algorithm<I, O>` 实现强类型输入和输出，并由 `AlgorithmProvider` 提供元数据、校验和动态调用入口。`ProviderCatalog` 是内置算法的统一目录。

算法执行时不直接操作 UI，而是通过 `AlgorithmContext` 发布结构化事件：

```text
Algorithm
   ↓ emit event
ExecutionEvent
   ↓
EventReducer<S>
   ↓
不可变视图状态 S
   ↓
JavaFX 实时动画 / 时间轴回放 / 其他事件消费者
```

`EventReducer` 使同一串事件可以被重放、单步执行或定位到任意可视帧。`ExecutionStatistics` 同时归约执行时间、事件数、可视帧数，以及比较、写入、交换、访问、回溯和旋转等算法指标。

JavaFX 客户端使用每次执行独立的会话、有界事件队列和 `PlaybackController`，支持：

- 执行、暂停、恢复和取消
- 动态播放速度
- 时间轴定位和重放；回放引擎支持前后单步
- 排序、AVL 树、数组迷宫生成与寻路、图迷宫生成和图遍历动画
- 执行记录导出与同输入结果对比

## 环境要求

- JDK 21
- Maven 3.9+
- 启动 JavaFX 客户端时需要可用的图形桌面环境

检查环境：

```bash
java -version
mvn -version
```

## 测试与打包

以下命令均在仓库根目录执行。

```bash
# 运行全部测试
mvn test

# 清理、测试并验证全部模块
mvn clean verify

# 打包全部模块
mvn clean package

# 跳过测试快速打包
mvn clean package -DskipTests
```

只构建某个模块及其依赖：

```bash
mvn -pl core -am package
mvn -pl algorithms -am package
mvn -pl client -am package
mvn -pl server -am package
mvn -pl other -am package
```

构建产物位于各模块的 `target/` 目录。

## 启动 JavaFX 客户端

首次按单模块启动前，先将项目模块安装到本地 Maven 仓库：

```bash
mvn -pl client -am install -DskipTests
```

通过通用执行插件启动：

```bash
mvn -f client/pom.xml exec:java
```

IDE 入口：

```text
com.majortom.algorithms.launcher.LauncherMain
```

## 启动 Spring Boot 服务端

`server` 目前提供 Spring Boot 应用骨架，并且只依赖 `core`。安装 reactor 依赖后启动：

```bash
mvn -pl server -am install -DskipTests
mvn -f server/pom.xml spring-boot:run
```

IDE 入口：

```text
com.majortom.algorithms.server.AlgorithmsServerApplication
```

## 启动 other 示例

`other` 模块的 Maven 默认入口为贪吃蛇示例，需要交互式终端和 ANSI 支持：

```bash
mvn -pl other -am install -DskipTests
mvn -f other/pom.xml exec:java
```

## 添加算法

1. 在 `algorithms` 中定义强类型 Input、Output 和结构化 Event。
2. 实现 `Algorithm<I, O>`，通过 `AlgorithmContext` 发布可回放事件。
3. 提供 `AlgorithmProvider`，并注册到 `ProviderCatalog`。
4. 需要可视化时，在消费端实现对应的 `EventReducer<ViewState>` 和渲染器。
5. 为结果、事件顺序和边界输入补充测试。

详细架构说明见[系统架构设计文档](docs/系统架构设计文档.md)。
