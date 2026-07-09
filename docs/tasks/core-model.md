# `:core:model` 最小任务列表

模块职责：跨 Domain、Data 和 Feature 使用的稳定业务模型。

依赖模块：`:core:common`。

需求：`PRD-DATA-001` 至 `PRD-DATA-008`。

## 任务

- [x] `MODEL-001` 创建模块并添加 `:core:common` 依赖。验证：`:core:model:compileDebugKotlin`。
- [x] `MODEL-002` 定义 Collection、ErrorItem、Tag、TutorSession、TutorMessage、Exercise 的强类型 UUID value class。验证：值对象测试。
- [x] `MODEL-003` 定义掌握状态、错误状态、试卷等级、练习难度、批改结果和 Provider 类型枚举。验证：持久化值映射测试。
- [x] `MODEL-004` 定义 `CollectionSummary`、`ErrorItem`、`ErrorItemSummary`、`StoredImage` 和标签树模型。验证：Fixture 构造测试。
- [x] `MODEL-005` 定义 `ErrorItemDraft` 与按逻辑字段分组的 `ErrorItemPatch`。验证：空正文和非法标签数量校验测试。
- [x] `MODEL-006` 定义 `ErrorItemQuery`，包含关键词、题集、掌握状态、时间、标签、年级和试卷等级。验证：默认查询测试。
- [x] `MODEL-007` 定义学生资料、AI 配置摘要、设备摘要和迁移进度模型。验证：序列化边界测试。
- [x] `MODEL-008` 定义统计模型，明确 `NEEDS_REVIEW` 不进入正确率分母。验证：统计模型计算测试。
- [x] `MODEL-009` 定义跨 Feature 的 `NavigationIntent`，不得引用 Compose 或 Android 类型。验证：依赖检查。

## 模块完成条件

- [x] `:core:model:testDebugUnitTest` 通过。
- [x] 模型中不存在 Room Entity、网络 DTO 或 Protobuf 生成类型。

