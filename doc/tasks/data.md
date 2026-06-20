# `:data` 最小任务列表

模块职责：实现 Domain Repository，协调 Storage、AI 和 Transfer。

依赖模块：`:domain`、`:core:model`、`:core:storage`、`:ai:api`、`:transfer:runtime`。

需求：`PRD-LIB-*`、`PRD-TAG-*`、`PRD-STAT-*`、`PRD-TUT-*`、`PRD-PRO-*`。

## 任务

- [x] `DATA-001` 创建模块和 Hilt Repository Binding 骨架。验证：`:data:compileDebugKotlin`。
- [x] `DATA-002` 实现 Room Entity 与 Domain Collection/ErrorItem Mapper。验证：双向映射测试。
- [x] `DATA-003` 实现 Tag、Tutor、Exercise 和统计 Mapper。验证：枚举兼容测试。
- [x] `DATA-004` 实现 `CollectionRepository`，保证删除题集事务一致。验证：Repository 集成测试。
- [x] `DATA-005` 实现 `ErrorItemRepository` 的分页、详情、创建、Patch 和批删。验证：Fake/Room 集成测试。
- [x] `DATA-006` 协调图片正式写入与数据库事务；失败时不留下孤立记录。验证：故障注入测试。
- [x] `DATA-007` 实现 `TagRepository`，标准标签只读、自定义标签可写。验证：约束测试。
- [x] `DATA-008` 实现 `TutorRepository` 和会话 sequence 分配。验证：并发追加消息测试。
- [x] `DATA-009` 实现练习创建、批改、人工覆盖和最终结果读取。验证：覆盖后统计测试。
- [ ] `DATA-010` 实现 `StatsRepository`，直接暴露 Room 聚合 Flow。验证：数据变化自动更新。
- [ ] `DATA-011` 实现 `AiConfigurationRepository`，协调 DataStore 与 Keystore。验证：验证失败不激活配置。
- [ ] `DATA-012` 实现 `AiTutorGateway` Provider 路由和错误归一化。验证：Gemini/OpenAI Fake 路由测试。
- [ ] `DATA-013` 实现 `ProfileRepository` 及学生资料更新。验证：入学年份校验测试。
- [ ] `DATA-014` 实现清除学习数据和恢复出厂设置协调器。验证：保留范围仪器测试。
- [ ] `DATA-015` 将 Transfer Runtime 映射为 `DeviceTransferRepository`。验证：状态和命令映射测试。
- [ ] `DATA-016` 添加所有 Repository 的 Hilt Binding 和替换测试模块。验证：Hilt 测试注入。

## 模块完成条件

- [ ] `:data:testDebugUnitTest` 和 Repository 仪器测试通过。
- [ ] Feature 无需感知 Entity、DTO、Provider 或 Socket。
