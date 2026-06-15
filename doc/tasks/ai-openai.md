# `:ai:openai` 最小任务列表

模块职责：OpenAI-compatible Chat Completions/SSE Provider 适配。

依赖模块：`:ai:api`、`:core:network`、`:core:common`。

需求：`PRD-AI-001` 至 `PRD-AI-010`、`PRD-TUT-003`。

## 任务

- [x] `OPENAI-001` 创建模块和 `OpenAiCompatibleProvider` 骨架。验证：`:ai:openai:compileDebugKotlin`。
- [x] `OPENAI-002` 实现配置验证最小 Chat Completions 请求。验证：MockWebServer 请求测试。
- [x] `OPENAI-003` 实现非流式 `/chat/completions` 请求和兼容响应提取。验证：标准与缺少可选字段测试。
- [x] `OPENAI-004` 实现文本 part 与 `image_url` data URL 的图片分析请求。验证：JSON 结构测试。
- [x] `OPENAI-005` 实现 `stream=true` SSE delta 和 `[DONE]` 处理。验证：跨 buffer UTF-8、结束和取消测试。
- [ ] `OPENAI-006` 接入图片模型与辅导模型的独立选择。验证：四类操作使用正确模型。
- [ ] `OPENAI-007` 将常见兼容服务的错误形状归一化为 `AiError`。验证：字符串、JSON 和空正文测试。
- [ ] `OPENAI-008` 接入 EndpointPolicy，覆盖初始 URL、DNS 和重定向。验证：`AC-AI-004`。
- [ ] `OPENAI-009` 实现规定的单次安全重试，流开始后不重试。验证：请求次数测试。
- [ ] `OPENAI-010` 添加 Key、Authorization、图片和正文脱敏测试。验证：`AC-AI-005`。

## 模块完成条件

- [ ] `:ai:openai:testDebugUnitTest` 通过。
- [ ] 至少一个标准 OpenAI-compatible Mock Server 完成验证、图片和流式测试。
