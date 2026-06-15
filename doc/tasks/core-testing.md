# `:core:testing` 最小任务列表

模块职责：全工程共享 Fake、Fixture、协程规则和架构测试工具。

依赖模块：`:core:common`、`:core:model`、`:domain`、`:ai:api`。

需求：`PRD-NFR-008`、`AC-NFR-002`。

## 任务

- [x] `TESTING-001` 创建测试夹具模块并配置仅供测试使用的依赖。验证：`:core:testing:compileDebugKotlin`。
- [x] `TESTING-002` 实现 `MainDispatcherRule`、`TestClock`、`FixedUuidGenerator`。验证：示例单元测试。
- [x] `TESTING-003` 实现 Collection、ErrorItem、Tag、Session、Exercise Fixture Builder。验证：默认 Fixture 可构造。
- [x] `TESTING-004` 实现 Fake Collection/ErrorItem/Tag Repository。验证：Flow 和失败注入测试。
- [x] `TESTING-005` 实现 Fake Tutor/Stats/Profile/AI Configuration Repository。验证：状态变化测试。
- [x] `TESTING-006` 实现 `FakeAiTutorGateway`，支持非流式、增量、失败和取消脚本。验证：脚本执行测试。
- [x] `TESTING-007` 实现 `FakeDeviceTransferRepository` 和可控迁移状态流。验证：状态推进测试。
- [x] `TESTING-008` 添加模块依赖架构断言：Feature 不互相依赖，纯 JVM 模块不依赖 Android。验证：根架构测试。

## 模块完成条件

- [x] 所有 Feature 可只依赖 `:core:testing` 完成 ViewModel 测试。
- [x] 架构测试能对非法依赖产生失败。
