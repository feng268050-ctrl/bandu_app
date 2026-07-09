# `:ai:api` 最小任务列表

模块职责：供应商无关的 AI 请求、响应、流事件、错误和提示词协议。

依赖模块：`:core:common`、`:core:model`。

需求：`PRD-AI-*`、`PRD-TUT-*`、`PRD-CAP-006`。

## 任务

- [x] `AIAPI-001` 创建纯 Kotlin 模块并禁止 Android 依赖。验证：`:ai:api:test`。
- [x] `AIAPI-002` 定义 `AiProvider`、Provider Registry 和 `ResolvedAiConfiguration`。验证：API 编译测试。
- [x] `AIAPI-003` 定义图片分析请求和 `AnalyzedQuestion`，包含九个输出字段。验证：模型校验测试。
- [x] `AIAPI-004` 定义 Tutor 请求、上下文消息和 `AiStreamEvent`。验证：事件顺序测试。
- [x] `AIAPI-005` 定义练习生成、练习批改请求与三态结果。验证：置信度边界测试。
- [x] `AIAPI-006` 定义 `AiError` 并提供供应商异常映射入口。验证：错误分类测试。
- [x] `AIAPI-007` 实现九标签图片分析响应解析器。验证：缺标签、未知状态、超过 5 标签测试。
- [x] `AIAPI-008` 实现练习三标签和批改三标签解析器。验证：非法 grade/confidence 测试。
- [x] `AIAPI-009` 定义四类 PromptType、必需占位符和默认模板加载接口。验证：模板列表测试。
- [x] `AIAPI-010` 实现提示词验证、渲染和未知占位符拒绝。验证：`AC-AI-003`。
- [x] `AIAPI-011` 实现固定错题与最近消息的 24,000 字符 ContextBuilder。验证：优先级和完整消息裁剪测试。
- [x] `AIAPI-012` 定义重试策略决策器，取消、鉴权、格式错误不重试。验证：错误矩阵测试。

## 模块完成条件

- [x] `:ai:api:test` 通过。
- [x] 模块不包含 OkHttp、Gemini 或 OpenAI 专有类型。
