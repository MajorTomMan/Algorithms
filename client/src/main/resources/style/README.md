# 客户端样式

样式按作用区域归档。同一区域中的 `theme.css` 管配色和视觉状态，
`typography.css` 管字号与文字行为，`layout.css` 管尺寸、间距和对齐。
只创建区域实际需要的文件，不要求每个区域都有三类文件。

## 加载入口

保留以下路径，供 FXML、操作对话框、字体设置弹窗和菜单复用：

1. `theme.css`：按原顺序导入区域视觉样式。
2. `typography.css`：按原顺序导入区域文字样式。
3. `font-settings.css`：字体设置弹窗，以及用户自定义文字颜色。
4. `workbench-layout.css`：按原顺序导入区域布局样式。
5. `popup.css`：ComboBox 和 ContextMenu 的独立 Popup 场景。

`theme.css`、`typography.css`、`workbench-layout.css` 三个清单中的区域文件顺序
保留了原有覆盖关系，请不要按文件名重新排序。
主界面与对话框继续加载这些入口；区域文件不需要在 Java 或 FXML 中逐个注册。

## 按区域查找

| 目录 | 作用区域与文件 |
| --- | --- |
| `dialogs/` | 操作对话框：`theme.css` |
| `catalog/` | 结构与算法目录：`theme.css` |
| `shell/` | 顶栏、工作区、导航：`theme.css`、`navigation.css`、`typography.css`、`layout.css`、`header-layout.css` |
| `controls/` | 左侧操作面板：`theme.css`、`typography.css`、`layout.css` |
| `practice/` | 练习工作区：`theme.css` |
| `viewport/` | 画布背景、工具栏、选择与当前步骤提示：`theme.css`、`typography.css`、`overlay-layout.css`、`toolbar-layout.css` |
| `inspector/` | 右侧检查器、诊断、快照预览：`theme.css`、`snapshot-preview.css`、`typography.css`、`layout.css` |
| `playback/` | 结构历史、执行时间线、播放控制：`history-theme.css`、`theme.css`、`typography.css`、`history-layout.css`、`layout.css` |
| `visualization/` | 可视化实体与语义状态：`labels.css`、`array.css`、`structures.css`、`algorithm-states.css` |
| `logs/` | 虚拟化日志列表：`theme.css` |
| `runtime/` | 运行概览：`theme.css`、`typography.css`、`layout.css` |
| `memory/` | 结构页和算法页共用的内存检查器：`theme.css`、`typography.css`、`layout.css` |
| `observations/` | 算法观察提示：`theme.css`、`typography.css`、`layout.css` |
| `shared/` | 跨区域基础规则、文字适配、响应式密度及后置覆盖，见下文 |

## 跨区域规则与覆盖

`shared/content-layout.css` 保留原有集中内容几何规则；
`shared/shell-layout.css` 保留从 FXML 移出的跨区域语义布局；
`shared/responsive.css` 管紧凑与窄窗口的布局密度；
`shared/typography.css` 管多个区域共用的省略、换行行为。

`shared/theme-overrides.css` 是后置覆盖，其中包含 AtlantaFX 深色主题上的白色检查器、
诊断面板、画布及其他已有覆盖规则。它不能直接移到基础样式前面。
该文件保留 `url("grid-dot.png")`。JavaFX 21 将导入规则的资源 URL 基准设为
入口清单 `style/theme.css`，因此图片继续位于 `style/grid-dot.png`。
区域文件通过清单加载，不作为独立 stylesheet 使用。

跨区域组合选择器保留完整规则，例如文字角色同时涉及面板标题、检查器和历史标题。
不要为了归档将组合选择器复制到多个文件，也不要直接删除重复选择器：
部分重复规则承担后置覆盖或状态优先级。

## 维护边界

- 新样式优先加入对应区域、对应职责的文件。
- 基础字号由 `FontSettingsService` 管理；主框架尺寸和可见性由 `WorkbenchUiFramework` 管理。
- 可视化实体坐标与绘制几何由 Java 管理，CSS 表达已有的视觉语义。
- 本次只搬移已有规则，保留了少量历史上混合的颜色、字号和布局属性；新增规则遵循职责划分。
- `font-settings.css` 和 `popup.css` 已对应独立区域，继续单独维护。
