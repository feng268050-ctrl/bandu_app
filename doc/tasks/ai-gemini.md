# `:ai:gemini` 最小任务列表

模块职责：Gemini REST/SSE Provider 适配。

依赖模块：`:ai:api`、`:core:network`、`:core:common`。

需求：`PRD-AI-001` 至 `PRD-AI-004`、`PRD-TUT-003`。

## 任务

- [x] `GEMINI-001` 创建模块和 `GeminiAiProvider` 骨架。验证：`:ai:gemini:compileDebugKotlin`。
- [x] `GEMINI-002` 实现配置验证最小请求，Key 通过 Header 发送。验证：MockWebServer 请求测试。
- [x] `GEMINI-003` 实现 `generateContent` 文本请求和响应提取。验证：成功与空响应测试。
- [x] `GEMINI-004` 实现 JPEG `inline_data` 图片分析请求。验证：MIME、Base64 和模型路径断言。
- [x] `GEMINI-005` 实现 `streamGenerateContent?alt=sse` 增量解析。验证：分片、结束、取消测试。
- [x] `GEMINI-006` 接入图片分析、辅导、练习生成和批改四类 Prompt。验证：模型选择断言。
- [x] `GEMINI-007` 将 HTTP、鉴权、限流、超时和格式错误映射为 `AiError`。验证：错误矩阵测试。
- [x] `GEMINI-008` 实现规定的单次安全重试，收到流增量后不重试。验证：请求次数测试。
- [x] `GEMINI-009` 添加日志脱敏测试，Key、图片和正文不出现在日志。验证：`AC-AI-005`。

## 模块完成条件

- [x] `:ai:gemini:testDebugUnitTest` 通过。
- [x] Gemini 配置验证、图片分析和流式辅导适配测试通过。
