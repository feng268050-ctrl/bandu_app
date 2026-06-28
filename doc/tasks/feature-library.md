# `:feature:library` 最小任务列表

模块职责：题集、错题列表、搜索筛选、详情编辑和删除。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-LIB-*`、`AC-LIB-*`。

## 任务

- [x] `LIBRARY-001` 创建题集列表、错题列表、错题详情三个 Contract/Route。验证：模块编译。
- [x] `LIBRARY-002` 实现题集列表名称、数量和更新时间展示。验证：Compose 测试。
- [x] `LIBRARY-003` 实现新建和重命名题集。验证：成功、重名、空名称测试。
- [x] `LIBRARY-004` 实现题集删除确认，非空时显示影响范围。验证：删除交互测试。
- [x] `LIBRARY-005` 实现 Paging 错题卡片、缩略图、摘要、标签、时间和掌握状态。验证：截图测试。
- [x] `LIBRARY-006` 实现关键词 300 ms debounce 搜索。验证：只发出最后查询测试。
- [x] `LIBRARY-007` 实现掌握、时间、标签、年级、试卷等级、题集组合筛选。验证：`AC-LIB-002`。
- [x] `LIBRARY-008` 实现多选、全不选和批量删除数量确认。验证：仅删除选中 ID。
- [x] `LIBRARY-009` 实现详情图片、题目、答案、解析、错误信息、标签、元数据和笔记展示。验证：Compose 测试。
- [x] `LIBRARY-010` 实现全部字段分组编辑和进程重开后持久化。验证：`AC-LIB-001`。
- [x] `LIBRARY-011` 实现三态掌握状态、单条删除和删除后返回。验证：ViewModel 测试。
- [x] `LIBRARY-012` 从详情发出绑定错题的 AI 辅导导航意图。验证：导航意图测试。
- [x] `LIBRARY-013` 接入 Markdown/GFM/LaTeX 展示。验证：`AC-LIB-003`。
- [x] `LIBRARY-014` 添加 5000 条列表滚动和筛选 Macrobenchmark 场景。验证：性能门槛。

## 模块完成条件

- [x] `:feature:library:testDebugUnitTest`、Compose 测试和性能场景通过。
- [x] 不实现打印、导出或回收站。
