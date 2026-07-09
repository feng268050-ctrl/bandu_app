# `:core:network` 最小任务列表

模块职责：受控 HTTP、SSE、端点安全、超时和日志脱敏。

依赖模块：`:core:common`。

需求：`PRD-AI-008` 至 `PRD-AI-010`、`PRD-SEC-004`。

## 任务

- [x] `NETWORK-001` 创建模块并接入 OkHttp、MockWebServer。验证：`:core:network:compileDebugKotlin`。
- [x] `NETWORK-002` 定义 `EndpointPolicy`、允许/拒绝结果和拒绝原因。验证：接口编译测试。
- [x] `NETWORK-003` 实现 HTTPS 默认允许、系统证书校验和禁止忽略证书策略。验证：无效证书测试。
- [x] `NETWORK-004` 实现 localhost、回环、RFC1918、IPv6 ULA/link-local 判定。验证：地址参数化测试。
- [x] `NETWORK-005` 实现 DNS 全解析检查，只要包含公网地址即拒绝 HTTP。验证：混合 DNS 测试。
- [x] `NETWORK-006` 对每次连接和每次重定向重新执行端点策略。验证：公网重定向和 HTTPS 降级测试。
- [x] `NETWORK-007` 创建 AI 专用 OkHttpClient，配置连接、读取、写入和调用超时。验证：超时测试。
- [x] `NETWORK-008` 实现 UTF-8 安全 SSE reader，支持跨 buffer 字符和 `[DONE]`。验证：MockWebServer 分片测试。
- [x] `NETWORK-009` 实现 Header、URL 和错误响应的日志脱敏拦截器。验证：API Key 和正文不可出现在日志。
- [x] `NETWORK-010` 禁止 OkHttp 自动重试写请求，暴露由 Provider 控制的重试辅助器。验证：失败请求次数断言。

## 模块完成条件

- [x] `:core:network:testDebugUnitTest` 通过。
- [x] `AC-AI-004` 和 `AC-AI-005` 的网络侧测试通过。
