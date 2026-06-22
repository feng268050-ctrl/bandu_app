# `:feature:devices` 最小任务列表

模块职责：设备发现、配对、信任管理和完整数据迁移界面。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-DEV-*`、`AC-DEV-*`。

## 任务

- [x] `DEVICES-001` 创建 Feature 模块和设备/迁移 Contract。验证：模块编译。
- [ ] `DEVICES-002` 展示本机名称、身份指纹、附近设备和已配对设备。验证：Compose 测试。
- [ ] `DEVICES-003` 实现手动开始/停止发现，离开页面停止。验证：ViewModel 命令测试。
- [ ] `DEVICES-004` 实现目标端接收模式、6 位码和 5 分钟倒计时。验证：过期 UI 测试。
- [ ] `DEVICES-005` 实现源端选择设备、输入配对码和配对错误展示。验证：错误/限流状态测试。
- [ ] `DEVICES-006` 配对后显示双方身份确认，拒绝时不保存信任。验证：确认流程测试。
- [ ] `DEVICES-007` 实现已配对设备列表和解除配对确认。验证：ViewModel 测试。
- [ ] `DEVICES-008` 实现源端完整迁移影响说明和发送确认。验证：明确源保留/目标替换。
- [ ] `DEVICES-009` 实现目标端接收确认，显示将清除 API Key。验证：确认文案测试。
- [ ] `DEVICES-010` 展示阶段、总进度、大小、速度、错误和可续传状态。验证：Fake State 截图测试。
- [ ] `DEVICES-011` 实现双方最终确认、取消和失败后原数据安全提示。验证：状态机 UI 测试。
- [ ] `DEVICES-012` 成功页明确源数据保留、目标 API Key 需重新配置。验证：Compose 测试。
- [ ] `DEVICES-013` 添加完整 Fake Repository 流程测试，不引用 NSD/Socket 类型。验证：`AC-NFR-002`。

## 模块完成条件

- [ ] `:feature:devices:testDebugUnitTest` 和 Compose 测试通过。
- [ ] UI 不实现自动同步或后台持续扫描入口。
