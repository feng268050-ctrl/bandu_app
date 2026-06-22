# `:feature:tutor` 最小任务列表

模块职责：会话列表、流式辅导、追问、变式练习和批改。

依赖模块：`:domain`、`:core:model`、`:core:designsystem`。

需求：`PRD-TUT-*`、`AC-AI-002`、`AC-TUT-*`。

## 任务

- [x] `TUTOR-001` 创建会话列表和会话详情 Contract/Route。验证：模块编译。
- [ ] `TUTOR-002` 实现会话列表标题、绑定错题和更新时间。验证：Compose 测试。
- [ ] `TUTOR-003` 实现新建无绑定会话和从错题创建绑定会话。验证：ViewModel 测试。
- [ ] `TUTOR-004` 实现历史消息离线加载和 Markdown/LaTeX 展示。验证：无网络测试。
- [ ] `TUTOR-005` 实现文本发送、流式增量和 250 ms UI 批量刷新。验证：Fake Stream 测试。
- [ ] `TUTOR-006` 实现停止生成，保留已接收内容并允许继续追问。验证：`AC-AI-002`。
- [ ] `TUTOR-007` 实现“分步讲解”快捷操作。验证：生成正确用户消息。
- [ ] `TUTOR-008` 实现四档难度“生成类似练习”。验证：`AC-TUT-001`。
- [ ] `TUTOR-009` 实现练习答案输入、AI 三态批改和反馈展示。验证：三态测试。
- [ ] `TUTOR-010` 实现人工覆盖批改结果并显示修改状态。验证：`AC-TUT-002`。
- [ ] `TUTOR-011` AI 配置缺失时保存并恢复原会话操作。验证：只恢复一次。
- [ ] `TUTOR-012` 实现删除会话确认，级联消息/练习但不删除错题。验证：ViewModel 测试。
- [ ] `TUTOR-013` 对长会话滚动、流式 Markdown 和进程重开恢复进行测试。验证：Compose/状态恢复测试。

## 模块完成条件

- [ ] `:feature:tutor:testDebugUnitTest` 和流式 Compose 测试通过。
- [ ] 练习仅存在于会话内，不增加独立一级页面。
