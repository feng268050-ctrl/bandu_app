## Context

参考文档：`docs/bandu_ai_auto_model_routing_design.md`。  
当前 Flutter 侧 `ai_config` 仅支持单一 `AiServiceConfig`（名称、Base URL、model、掩码 Key、`isDefault`），无法表达 Auto、系统模型、按用途偏好，也无法展示路由结果。

约束：

- App 不执行 Auto 路由，不保存明文 API Key。
- 不硬编码 Gemini / DeepSeek 等供应商名称；只依赖用途与能力。
- 服务端契约在 `bandu_web`；本设计只定义 App 如何消费与展示。

## Goals / Non-Goals

**Goals:**

- 提供模型目录 UI（Auto / 系统 / 我的模型）。
- 按用途管理 Auto / MANUAL 偏好并与服务端同步。
- 用户模型 CRUD（Key 掩码）；系统模型只读。
- AI 请求透传 `purpose` + 偏好；结果区展示 resolved 模型与降级提示。
- 从旧 `isDefault` 配置平滑迁移到新模型/偏好模型。

**Non-Goals:**

- 服务端 `resolveAiModel`、Provider 调用、Key 加密、健康熔断、动态评分。
- 第一版不做复杂本地缓存失效策略以外的离线完整模型管理。

## Decisions

### 1. 领域模型替换 `AiServiceConfig`

引入与设计文档对齐的领域类型（App 侧）：

- `AiModelKind`: `auto` | `system` | `user`
- `AiPurpose`: `visionAnalyze` | `pdfImport` | `tutor` | `questionGenerate` | `answerExplain`
- `AiModelSummary`: id、displayName、capabilities、kind、enabled、参与 Auto 标记、maskedKey（仅 user）
- `AiModelCatalog`: auto 入口 + systemModels + userModels
- `AiPurposePreference`: mode、selectedModelId、allowUserModelsInAuto、allowFallbackInManual

**替代方案**：继续扩展 `AiServiceConfig` → 拒绝，语义无法覆盖系统/Auto。

### 2. API 客户端分层

沿用现有五层：`application` 定义 repository；`framework` 实现 HTTP；`conversion` 做 DTO 映射。

新服务建议：

- `AiModelApiService` → `/ai/models`
- `AiPreferenceApiService` → `/ai/preferences`

旧 `/ai/configs` 类接口在服务端废弃后由 App 移除；迁移期可通过 feature flag 或版本探测。

### 3. UI 结构

- 设置入口「AI 模型」：目录页（§12.1）+ 「按功能偏好」分区（§12.2）+ 添加模型。
- 编辑页：§12.3 表单字段；编辑时 Key 留空表示不修改。
- 拍题 / 辅导 / PDF 等结果或会话头部：显示 `由 Auto 选择：{name}` 或降级文案（§12.4）。展示数据来自 AI 响应中的 `resolvedModel` 字段（契约由服务端提供）。

### 4. 请求透传

各 AI 调用在请求体增加：

```json
{
  "purpose": "VISION_ANALYZE",
  "modelSelection": { "mode": "AUTO" }
}
```

或 MANUAL + `modelId`。App 从本地已同步的偏好组装；若未加载则默认 Auto。

### 5. 与服务端边界

| App | Server (`bandu_web`) |
|-----|----------------------|
| 列表/偏好 UI | 系统模型与策略 |
| 用户模型 CRUD 表单 | Key 加密与校验 |
| 透传 purpose/偏好 | Router + Provider + 降级 |
| 展示 resolved 结果 | 返回 resolvedModel / fallback 元数据 |

## Risks / Trade-offs

- [服务端 API 未就绪] → App 先按契约实现 Fake/Mock repository 与 UI；联调后再切真实 HTTP。
- [旧默认配置用户数据] → 迁移说明：首次进入新页时若仅有旧 config，引导用户或调用服务端迁移接口（若提供）。
- [响应缺少 resolved 字段] → UI 降级为仅显示「Auto」或用户所选模型，不崩溃。

## Migration Plan

1. 落地领域模型与 Fake repository + 单元测试。
2. 替换 AI 配置 UI；保留入口路径兼容。
3. 各 AI 请求增加 purpose/偏好字段。
4. 联调 `bandu_web`；通过验收清单中的 App 项。
5. 删除旧 `isDefault` 唯一默认逻辑。

回滚：保留旧 API client 分支或 feature flag；UI 可回退到旧 `ai_config` 页。

## Open Questions

- 服务端 AI 响应中 `resolvedModel` / `fallback` 的精确 JSON 字段名（联调时对齐）。
- 旧 MobileAiConfig 是否由服务端一次性迁移，或 App 仅展示空目录直至用户重建。
