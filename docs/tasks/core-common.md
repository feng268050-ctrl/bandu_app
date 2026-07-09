# `:core:common` 最小任务列表

模块职责：跨模块基础类型、时间、UUID、Dispatcher、结果和日志抽象。

依赖模块：无 Android 业务模块。

需求：`PRD-DATA-006`、`PRD-DATA-007`、`PRD-SEC-004`。

## 任务

- [x] `COMMON-001` 创建纯 Kotlin/Android Library 模块并配置最小依赖。验证：`:core:common:compileDebugKotlin`。
- [x] `COMMON-002` 定义 `Clock` 与 `SystemClock`，统一返回 UTC epoch millis。验证：Clock 单元测试。
- [x] `COMMON-003` 定义 `UuidGenerator` 与随机 UUID 实现。验证：格式和唯一性单元测试。
- [x] `COMMON-004` 定义 `DispatcherProvider`，提供 Main、IO、Default。验证：测试实现可注入 `TestDispatcher`。
- [x] `COMMON-005` 定义统一 `AppResult`/错误分类，不携带 UI 文案。验证：成功、失败映射测试。
- [x] `COMMON-006` 定义 `AppLogger` 接口和字段白名单清洗器。验证：敏感键和值脱敏测试。
- [x] `COMMON-007` 实现 Release 日志最小化策略和 Debug 本地日志实现。验证：Release 单元测试不输出正文。
- [x] `COMMON-008` 添加 `Closeable`/Coroutine 取消辅助函数，禁止吞掉 `CancellationException`。验证：协程取消测试。

## 模块完成条件

- [x] `:core:common:testDebugUnitTest` 通过。
- [x] 模块不依赖 Room、OkHttp、Compose 或 Feature。

