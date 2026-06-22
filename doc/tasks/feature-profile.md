# `:feature:profile` 最小任务列表

模块职责：学生资料、AI 配置、设备名称、数据管理和关于。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-PRO-*`、`PRD-AI-*`。

## 任务

- [x] `PROFILE-001` 创建“我的”首页和五个设置分区 Contract/Route。验证：模块编译。
- [x] `PROFILE-002` 实现昵称、教育阶段、入学年份编辑和校验。验证：未来年份拒绝测试。
- [x] `PROFILE-003` 实现设备名称编辑，限制长度并实时保存。验证：ViewModel 测试。
- [x] `PROFILE-004` 实现 Gemini/OpenAI-compatible Provider 选择和公共配置字段。验证：不显示 Azure。
- [x] `PROFILE-005` 实现 API Key 输入、掩码和“不读取明文回显”。验证：UI 状态测试。
- [x] `PROFILE-006` 实现图片模型和辅导模型独立配置。验证：保存模型测试。
- [ ] `PROFILE-007` 实现连接验证，成功后激活、失败保留未激活草稿。验证：Fake Gateway 测试。
- [ ] `PROFILE-008` 实现私有 HTTP 高级开关和风险确认。验证：未确认不能启用。
- [ ] `PROFILE-009` 实现四类提示词编辑、占位符错误和恢复默认。验证：`AC-AI-003`。
- [ ] `PROFILE-010` 实现 AI 数据发送说明确认。验证：首次请求前可见。
- [ ] `PROFILE-011` 实现“清除学习数据”影响说明和指定文本确认。验证：保留 Profile/AI/Key/设备身份。
- [ ] `PROFILE-012` 实现“恢复出厂设置”影响说明和指定文本确认。验证：全部清除并生成新身份。
- [ ] `PROFILE-013` 实现关于页：名称、版本、隐私、开源许可和图标来源。验证：Compose 测试。
- [ ] `PROFILE-014` 实现配置成功返回结果，供 PendingOperationCoordinator 恢复操作。验证：Capture/Tutor 集成测试。

## 模块完成条件

- [ ] `:feature:profile:testDebugUnitTest` 和数据清除仪器测试通过。
- [ ] `AC-AI-003` 至 `AC-AI-005`、`PRD-PRO-*` 通过。
