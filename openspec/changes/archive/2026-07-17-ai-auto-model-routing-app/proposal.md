## Why

当前 Flutter App 的 AI 配置仍是“单一默认服务配置”模型，无法支持系统内置模型、用户自定义模型、按业务用途选择 Auto/手动模型，以及展示 Auto 实际选用模型。`docs/bandu_ai_auto_model_routing_design.md` 已定义统一方案；本变更只落地 **bandu_app（Flutter）** 侧职责：展示、选择、偏好与请求透传，路由与 Key 托管留在 `bandu_web`。

## What Changes

- 将现有 `AiServiceConfig` / “默认配置” UI 升级为设计文档中的 **AI 模型目录**：Auto + 系统模型 + 我的模型。
- 新增/改造按业务用途（拍题、PDF、辅导、出题、解析）的 **Auto / 手动模型偏好** 设置与同步。
- 用户可在 App 中 **新增、编辑、停用、删除** 自定义模型；API Key 仅提交服务端，界面只显示掩码。
- 发起 AI 请求时携带 `purpose` 与用户偏好（Auto 或指定 `modelId`）；**不在 App 内硬编码供应商或执行路由**。
- Auto/降级完成后，在相关流程中展示 **实际选用模型** 与降级提示。
- **不包含**：`bandu_web` 的 Router、系统模型入库、Key 加密、健康熔断、动态评分（由服务端变更单独完成）。

## Capabilities

### New Capabilities

- `ai-model-catalog`: 拉取并展示 Auto / 系统模型 / 用户模型列表，以及模型能力与内置标记。
- `ai-purpose-preferences`: 按用途读写 Auto/MANUAL 偏好，含是否允许用户模型参与 Auto、手动模式是否允许降级。
- `ai-user-model-management`: 用户自定义模型的新增、编辑（Key 留空不修改）、删除；系统模型不可删。
- `ai-request-model-context`: AI 请求透传 purpose 与模型偏好；展示 resolved 模型与降级提示。

### Modified Capabilities

- （无既有 openspec specs；当前 `ai_config` 功能作为实现基线迁移，不新增 delta spec。）

## Impact

- **代码**：`lib/application/features/ai_config/**`、相关 DTO/mapper、AI 调用入口（拍题、PDF、辅导、练习生成等）的请求字段与结果展示。
- **API 依赖**：`GET/POST/PATCH/DELETE /api/mobile/v1/ai/models`、`GET/PUT /api/mobile/v1/ai/preferences*`；需 `bandu_web` 先提供或并行交付契约。
- **非目标**：服务端路由算法、Provider 调用、Key 落库加密、熔断与评分。
- **仓库**：本仓库 `bandu_app`；服务端工作在 `~/workspace/bandu_web`。
