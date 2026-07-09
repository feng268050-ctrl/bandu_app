# `:core:designsystem` 最小任务列表

模块职责：Compose 主题、通用组件、Markdown/LaTeX 和统计图表。

依赖模块：`:core:model`、`:core:common`。

需求：`PRD-HOME-002`、`PRD-LIB-014`、`PRD-NFR-010`。

## 任务

- [x] `DESIGN-001` 创建 Compose Library 模块并接入 Compose BOM。验证：`:core:designsystem:compileDebugKotlin`。
- [x] `DESIGN-002` 定义浅色颜色、字体、圆角、间距和阴影 token。验证：Theme Preview/截图测试。
- [x] `DESIGN-003` 实现通用页面 Scaffold、卡片、空状态、加载状态和错误状态组件。验证：Compose 单元测试。
- [x] `DESIGN-004` 实现危险确认对话框和输入确认文本组件。验证：确认文本不匹配时按钮禁用。
- [x] `DESIGN-005` 实现标签 Chip、掌握状态 Badge、筛选 Chip 和分页加载项。验证：状态映射截图测试。
- [x] `DESIGN-006` 将 `markdown-it 14.1.0`、KaTeX 0.16.25、DOMPurify 3.3.1 固定打包到 assets。验证：Release APK 中资源存在且无远程 CDN。
- [x] `DESIGN-007` 实现受限 `MarkdownLatexView`，关闭网络、文件访问、弹窗和任意 URL 跳转。验证：恶意 Markdown 安全测试。
- [x] `DESIGN-008` 支持 GFM 表格、行内/块级 LaTeX 和流式批量刷新。验证：需求样例渲染截图测试。
- [x] `DESIGN-009` 实现饼图、柱状图和趋势图的本地 Compose 组件。验证：空数据和 6 个月数据截图测试。
- [x] `DESIGN-010` 添加 200% 字体缩放和 48 dp 触摸目标测试。验证：无文字截断的 Compose 测试。

## 模块完成条件

- [x] `:core:designsystem:testDebugUnitTest` 和截图测试通过。
- [x] Markdown 渲染不发起网络请求，不暴露 JavaScript Bridge。
