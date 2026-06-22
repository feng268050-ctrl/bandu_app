# `:feature:capture` 最小任务列表

模块职责：拍照、相册、裁剪、AI 分析、人工确认和保存。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-CAP-*`、`AC-CAP-*`。

## 任务

- [x] `CAPTURE-001` 创建 Feature 模块和 CaptureStage Contract。验证：状态模型测试。
- [x] `CAPTURE-002` 实现来源选择页面，仅提供拍照和相册。验证：无纯文本/文件入口测试。
- [x] `CAPTURE-003` 集成 CameraX 拍照和按需 CAMERA 权限。验证：允许/拒绝仪器测试。
- [ ] `CAPTURE-004` 集成 Photo Picker，不请求广泛媒体读取权限。验证：merged manifest。
- [ ] `CAPTURE-005` 实现裁剪、旋转页面和取消返回。验证：裁剪 UI 测试。
- [ ] `CAPTURE-006` 调用图片处理 UseCase并展示压缩进度/最低质量提示。验证：ViewModel 测试。
- [ ] `CAPTURE-007` 调用图片分析 AI，展示可重试中文错误。验证：错误矩阵测试。
- [ ] `CAPTURE-008` AI 配置缺失时保存 PendingOperation 并导航设置。验证：中断状态测试。
- [ ] `CAPTURE-009` 配置成功后自动恢复同一草稿分析且只执行一次。验证：`AC-CAP-003`。
- [ ] `CAPTURE-010` 实现确认页全部字段编辑和最多 5 个标签限制。验证：字段保存测试。
- [ ] `CAPTURE-011` 保存时要求题集，成功后发出错题详情导航。验证：Fake Repository 测试。
- [ ] `CAPTURE-012` 取消或失败后清理临时草稿，保存后只保留处理图。验证：`AC-CAP-002`。
- [ ] `CAPTURE-013` 防止重复点击导致重复错题。验证：并发保存测试。
- [ ] `CAPTURE-014` 添加拍照、相册到保存的完整 Compose/E2E 测试。验证：`AC-CAP-001`。

## 模块完成条件

- [ ] `:feature:capture:testDebugUnitTest` 和仪器测试通过。
- [ ] 模块不直接依赖具体 AI Provider 或 Storage 实现。
