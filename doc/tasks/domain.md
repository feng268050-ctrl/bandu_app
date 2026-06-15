# `:domain` 最小任务列表

模块职责：Repository 合约和不依赖基础设施的 UseCase。

依赖模块：`:core:common`、`:core:model`。

需求：全部业务需求，重点 `PRD-LIB-*`、`PRD-TUT-*`、`PRD-PRO-*`。

## 任务

- [x] `DOMAIN-001` 创建纯 Kotlin 模块并限制依赖范围。验证：`:domain:test`。
- [x] `DOMAIN-002` 定义 Collection、ErrorItem、Tag Repository 接口。验证：API 编译测试。
- [x] `DOMAIN-003` 定义 Tutor、Stats、Profile Repository 接口。验证：API 编译测试。
- [x] `DOMAIN-004` 定义 AI Configuration、AI Gateway 和 Pending Operation 接口。验证：API 编译测试。
- [x] `DOMAIN-005` 定义 Device Transfer Repository 接口和 TransferState。验证：状态模型测试。
- [x] `DOMAIN-006` 实现题集创建、改名、删除 UseCase 和名称校验。验证：Fake Repository 单元测试。
- [x] `DOMAIN-007` 实现错题创建、编辑、掌握状态和批量删除 UseCase。验证：校验和调用测试。
- [x] `DOMAIN-008` 实现标签创建、改名、删除 UseCase。验证：系统标签拒绝修改测试。
- [x] `DOMAIN-009` 实现 Tutor 会话、发送、停止、生成练习和批改 UseCase。验证：Fake Gateway 测试。
- [x] `DOMAIN-010` 实现 AI 配置验证、保存、提示词保存和恢复 UseCase。验证：占位符失败测试。
- [x] `DOMAIN-011` 实现清除学习数据和恢复出厂设置 UseCase，明确两者保留范围。验证：调用范围测试。
- [x] `DOMAIN-012` 实现设备发现、配对、发送、接收、取消和解除配对 UseCase。验证：Fake 状态测试。
- [x] `DOMAIN-013` 实现 `PendingOperationCoordinator`，AI 配置成功只恢复一次。验证：重复回调测试。

## 模块完成条件

- [x] `:domain:test` 全部通过。
- [x] 模块不依赖 Android、Room、OkHttp、Compose、NSD 或具体 Provider。
