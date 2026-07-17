# bandu AI 模型与 Auto 路由设计方案

> 目标：参考 Cursor 的“内置模型 + 用户自定义模型 + Auto 自动选择”设计，为 `bandu_app` 和 `bandu_web` 建立统一的 AI 模型管理与调度机制。  
> 适用范围：拍题分析、PDF 导入、AI 辅导、题目生成、答案解析等 AI 功能。  
> 当前原则：AI 模型选择与 API Key 管理由 `bandu_web` 负责，Flutter 只负责展示和选择。

---

## 1. 设计目标

Bandu 的 AI 模型体系需要同时支持：

```text
系统内置模型
+
用户自定义模型
+
Auto 自动选择
+
手动固定模型
+
按业务用途路由
+
模型不可用时自动降级
```

最终用户体验：

```text
默认：
使用 Auto

可选：
手动选择系统模型

可选：
新增自己的模型和 API Key

可选：
允许或禁止自定义模型参与 Auto
```

---

## 2. Cursor Auto 的可参考部分

Cursor 的 Auto 可以理解为：

```text
一个虚拟模型入口
+
平台维护的候选模型池
+
自动选择当前可用模型
```

Cursor 没有公开完整的 Auto 路由算法，因此不能假设其一定按照任务语义、价格、质量和延迟进行完整动态评分。

Bandu 不需要复刻 Cursor 的黑盒路由，应采用更透明、可控的策略：

```text
按业务用途筛选
→ 检查模型能力
→ 检查模型可用性
→ 按优先级选择
→ 失败时降级
```

---

## 3. 总体架构

```text
Flutter App
    ↓
Auto 或指定模型
    ↓
bandu_web AI Router
    ↓
用途识别
    ↓
能力过滤
    ↓
候选模型排序
    ↓
选择实际模型
    ↓
AI Provider
```

职责划分：

```text
bandu_app
=
展示模型列表
选择 Auto 或手动模型
新增用户模型
显示当前实际模型
展示错误和降级状态

bandu_web
=
保存系统模型
加密保存用户 API Key
解析用户偏好
执行 Auto 路由
调用 Provider
记录模型调用结果
执行失败降级
```

---

## 4. 模型类型

### 4.1 系统模型

由 `bandu_web` 管理：

```text
系统视觉模型
系统文本推理模型
系统备用模型
```

特点：

```text
API Key 由服务端保存
所有用户可用
用户不能删除
可以由管理员启用或停用
可以参与 Auto
```

示例：

```text
Gemini Vision
DeepSeek Chat
OpenAI Compatible
```

---

### 4.2 用户自定义模型

由用户在 App 中新增：

```text
Provider
Model Name
Base URL
API Key
能力声明
```

特点：

```text
只属于当前用户
API Key 加密保存在 bandu_web
完整 Key 不返回 App
用户可以编辑、停用和删除
可以选择是否参与 Auto
```

---

### 4.3 Auto

Auto 不是一个真实模型。

```text
modelId = auto
```

服务端收到 Auto 后执行模型路由。

Auto 可以：

```text
根据当前业务选择模型
根据模型能力过滤
根据健康状态跳过不可用模型
根据优先级选择
在失败时切换到备用模型
```

---

## 5. 按业务用途定义模型

不建议只设置一个全局默认模型。

应定义业务用途：

```text
VISION_ANALYZE
PDF_IMPORT
TUTOR
QUESTION_GENERATE
ANSWER_EXPLAIN
```

建议说明：

| 用途 | 说明 | 关键能力 |
|---|---|---|
| `VISION_ANALYZE` | 拍题、图片分析 | 图片输入、结构化输出 |
| `PDF_IMPORT` | PDF 页面识别和拆题 | 图片/长文本、长上下文 |
| `TUTOR` | AI 多轮辅导 | 多轮对话、文本推理 |
| `QUESTION_GENERATE` | 生成练习题 | 文本推理、结构化输出 |
| `ANSWER_EXPLAIN` | 答案解析和知识点总结 | 文本推理、稳定输出 |

请求中应明确携带用途，服务端不要完全依赖 Prompt 猜测。

示例：

```json
{
  "purpose": "VISION_ANALYZE",
  "model": "auto"
}
```

---

## 6. 系统默认模型设计

建议至少配置两个系统默认模型：

```text
系统视觉主模型
系统文本主模型
```

可选再增加：

```text
系统视觉备用模型
系统文本备用模型
```

示例：

```text
VISION_ANALYZE
→ Gemini Vision

PDF_IMPORT
→ Gemini Vision

TUTOR
→ DeepSeek Chat

QUESTION_GENERATE
→ DeepSeek Chat

ANSWER_EXPLAIN
→ DeepSeek Chat
```

不要在 Flutter 中硬编码具体供应商名称。

Flutter 只依赖能力和用途：

```text
视觉模型
文本推理模型
Auto
```

后续替换 Gemini 或 DeepSeek 时，不需要修改 App。

---

## 7. Auto 路由流程

### 7.1 第一步：确定用途

每个 AI 请求必须确定：

```text
AiPurpose
```

例如：

```text
拍题
→ VISION_ANALYZE

PDF 导入
→ PDF_IMPORT

AI 辅导
→ TUTOR
```

---

### 7.2 第二步：能力硬过滤

先排除无法完成任务的模型。

拍题模型至少要求：

```text
支持图片输入
支持结构化 JSON
当前启用
API Key 可用
健康状态正常
```

PDF 导入模型至少要求：

```text
支持图片或长文本
上下文长度足够
支持结构化输出
```

AI 辅导模型至少要求：

```text
支持多轮文本
上下文长度足够
文本推理能力达标
```

伪代码：

```ts
const candidates = models.filter((model) => {
  return model.enabled &&
    supportsPurpose(model, purpose) &&
    model.healthStatus !== "UNAVAILABLE";
});
```

能力不满足时，不能通过优先级或评分继续使用。

---

### 7.3 第三步：应用用户偏好

用户偏好包括：

```text
AUTO 或 MANUAL
选定模型
是否允许用户模型参与 Auto
固定模型不可用时是否允许降级
```

Auto 模式：

```text
mode = AUTO
selectedModelId = null
```

手动模式：

```text
mode = MANUAL
selectedModelId = model_xxx
```

---

### 7.4 第四步：候选模型排序

第一版使用确定性优先级：

```text
系统主模型
→ 系统备用模型
→ 用户模型
```

或者用户允许自定义模型优先时：

```text
用户指定偏好模型
→ 系统主模型
→ 系统备用模型
```

排序因素：

```text
业务优先级
系统配置 priority
用户偏好
模型健康状态
近期失败次数
```

---

### 7.5 第五步：调用与失败降级

示例：

```text
VISION_ANALYZE

系统视觉主模型
    ↓ 失败
系统视觉备用模型
    ↓ 失败
用户视觉模型
    ↓ 失败
返回 MODEL_UNAVAILABLE
```

失败类型需要区分。

可以降级：

```text
超时
限流
Provider 暂时不可用
5xx
连接失败
```

不应自动降级：

```text
请求参数错误
图片格式错误
用户权限错误
Prompt 业务错误
模型明确不支持该输入
```

---

## 8. Auto 与手动模式

### 8.1 Auto 模式

```text
用户选择 Auto
→ 服务端选择适合模型
→ 模型不可用时允许降级
```

优点：

```text
不需要用户理解模型差异
可以自动绕过故障
可以随服务端策略升级
```

---

### 8.2 手动模式

```text
用户指定某个模型
→ 固定调用该模型
```

默认行为：

```text
模型失败
→ 显示当前模型不可用
```

不建议在手动模式下无提示切换模型。

可增加选项：

```text
固定模型不可用时允许 Auto 降级
```

默认关闭。

---

## 9. 多轮会话的模型固定

独立任务可以每次重新选择：

```text
拍题
PDF 导入
练习生成
答案解析
```

AI 辅导属于多轮会话，建议会话开始时确定一次模型。

流程：

```text
新建辅导会话
    ↓
Auto 选择模型
    ↓
保存 resolvedModelId
    ↓
后续消息固定使用该模型
```

只有以下情况切换：

```text
用户主动切换
模型持续不可用
上下文超限
用户新建会话
```

避免：

```text
第一轮模型 A
第二轮模型 B
第三轮模型 C
```

造成回答风格和上下文理解不一致。

---

## 10. 数据库设计

### 10.1 枚举

```prisma
enum AiConfigScope {
  SYSTEM
  USER
}

enum AiSelectionMode {
  AUTO
  MANUAL
}

enum AiPurpose {
  VISION_ANALYZE
  PDF_IMPORT
  TUTOR
  QUESTION_GENERATE
  ANSWER_EXPLAIN
}

enum AiHealthStatus {
  UNKNOWN
  AVAILABLE
  DEGRADED
  UNAVAILABLE
}
```

---

### 10.2 模型配置

```prisma
model AiModelConfig {
  id             String         @id @default(cuid())
  scope          AiConfigScope
  ownerUserId    String?

  name           String
  provider       String
  modelName      String
  baseUrl        String?
  encryptedKey   String?

  enabled        Boolean        @default(true)
  healthStatus   AiHealthStatus @default(UNKNOWN)

  supportsVision Boolean        @default(false)
  supportsText   Boolean        @default(true)
  supportsJson   Boolean        @default(false)
  contextWindow  Int?

  priority       Int            @default(100)

  createdAt      DateTime       @default(now())
  updatedAt      DateTime       @updatedAt
}
```

说明：

```text
scope = SYSTEM
→ 系统模型

scope = USER
→ 用户自定义模型
```

---

### 10.3 用户模型偏好

```prisma
model UserAiPreference {
  id                      String          @id @default(cuid())
  userId                  String
  purpose                 AiPurpose
  mode                    AiSelectionMode @default(AUTO)
  selectedModelId         String?
  allowUserModelsInAuto   Boolean         @default(true)
  allowFallbackInManual   Boolean         @default(false)

  @@unique([userId, purpose])
}
```

---

### 10.4 系统路由策略

```prisma
model AiRoutePolicy {
  id               String    @id @default(cuid())
  purpose          AiPurpose
  primaryModelId   String
  fallbackModelIds Json
  enabled          Boolean   @default(true)

  @@unique([purpose])
}
```

示例：

```json
{
  "purpose": "VISION_ANALYZE",
  "primaryModelId": "gemini-vision",
  "fallbackModelIds": [
    "openai-vision",
    "user-models"
  ]
}
```

---

### 10.5 辅导会话模型

```prisma
model TutorSession {
  id               String   @id @default(cuid())
  userId           String
  requestedMode    String
  resolvedModelId  String
  createdAt        DateTime @default(now())
  updatedAt        DateTime @updatedAt
}
```

---

## 11. API Key 管理

### 系统模型

系统 Key 保存于：

```text
bandu_web 环境变量
Secret 文件
服务端数据库加密字段
```

### 用户模型

流程：

```text
Flutter 输入 API Key
    ↓ HTTPS
bandu_web
    ↓
AES-256-GCM 加密
    ↓
SQLite 保存密文
```

返回 App 时只显示：

```text
sk-****abcd
```

禁止：

```text
完整 API Key 返回 App
API Key 写入日志
API Key 写入 Flutter 配置
API Key 进入 APK
```

---

## 12. Flutter UI 设计

### 12.1 模型列表

```text
AI 模型

推荐
[ Auto                                      ✓ ]
  自动为当前任务选择合适模型

系统模型
[ 系统视觉模型                           内置 ]
[ 系统文本推理模型                       内置 ]

我的模型
[ DeepSeek V3                              ]
[ Gemini 2.5 Flash                         ]

                            [ + 添加模型 ]
```

---

### 12.2 Auto 开关

不建议只做一个全局布尔开关。

推荐：

```text
全局默认：
Auto

按功能覆盖：
拍题分析 → Auto
PDF 导入 → Auto
AI 辅导 → 手动指定 DeepSeek
```

设置示例：

```text
拍题分析
[ Auto ]

PDF 导入
[ Auto ]

AI 辅导
[ DeepSeek V3 ▼ ]
```

---

### 12.3 自定义模型表单

字段：

```text
配置名称
Provider
Model Name
Base URL
API Key
支持图片
支持文本
支持 JSON
是否启用
是否参与 Auto
```

API Key 规则：

```text
新增时必须输入
编辑时留空表示不修改
只显示掩码
```

---

### 12.4 显示实际模型

Auto 请求完成后可以显示：

```text
由 Auto 选择：Gemini 2.5 Flash
```

如果发生降级：

```text
主模型不可用，已切换到备用模型
```

不要只显示：

```text
Auto
```

而隐藏真实调用模型，否则不利于排查。

---

## 13. 服务端 API

### 13.1 查询模型

```http
GET /api/mobile/v1/ai/models
```

返回：

```json
{
  "data": {
    "auto": {
      "enabled": true
    },
    "systemModels": [],
    "userModels": []
  }
}
```

---

### 13.2 新增用户模型

```http
POST /api/mobile/v1/ai/models
```

```json
{
  "name": "My DeepSeek",
  "provider": "openai-compatible",
  "modelName": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com",
  "apiKey": "sk-...",
  "supportsVision": false,
  "supportsText": true,
  "supportsJson": true
}
```

---

### 13.3 修改模型

```http
PATCH /api/mobile/v1/ai/models/{id}
```

---

### 13.4 删除模型

```http
DELETE /api/mobile/v1/ai/models/{id}
```

系统模型禁止删除。

---

### 13.5 查询用户偏好

```http
GET /api/mobile/v1/ai/preferences
```

---

### 13.6 修改用途偏好

```http
PUT /api/mobile/v1/ai/preferences/{purpose}
```

Auto：

```json
{
  "mode": "AUTO",
  "modelId": null,
  "allowUserModelsInAuto": true
}
```

手动：

```json
{
  "mode": "MANUAL",
  "modelId": "model_xxx",
  "allowFallbackInManual": false
}
```

---

## 14. AI Router 接口

建议统一入口：

```ts
type ResolveAiModelInput = {
  userId: string;
  purpose: AiPurpose;
  requestedMode?: "AUTO" | "MANUAL";
  requestedModelId?: string;
};

type ResolvedAiModel = {
  model: AiModelConfig;
  requestedMode: "AUTO" | "MANUAL";
  resolvedBy: "USER" | "SYSTEM_POLICY" | "FALLBACK";
  fallbackLevel: number;
};
```

核心接口：

```ts
async function resolveAiModel(
  input: ResolveAiModelInput,
): Promise<ResolvedAiModel>
```

调用方：

```text
/analyze
/question-banks/import
/tutor
/practice/generate
```

全部通过 Router 获取模型，不能各自直接读取不同配置。

---

## 15. 第一版路由伪代码

```ts
async function resolveAiModel(
  input: ResolveAiModelInput,
): Promise<ResolvedAiModel> {
  const preference = await getPreference(
    input.userId,
    input.purpose,
  );

  if (preference.mode === "MANUAL") {
    const selected = await getModel(
      preference.selectedModelId,
    );

    assertModelUsable(selected, input.purpose);

    return {
      model: selected,
      requestedMode: "MANUAL",
      resolvedBy: "USER",
      fallbackLevel: 0,
    };
  }

  const policy = await getRoutePolicy(input.purpose);

  const candidates = await buildCandidateList({
    userId: input.userId,
    purpose: input.purpose,
    policy,
    allowUserModels:
      preference.allowUserModelsInAuto,
  });

  const selected = candidates.find(
    (model) =>
      model.enabled &&
      supportsPurpose(model, input.purpose) &&
      model.healthStatus !== "UNAVAILABLE",
  );

  if (!selected) {
    throw new AiModelUnavailableError(
      input.purpose,
    );
  }

  return {
    model: selected,
    requestedMode: "AUTO",
    resolvedBy: "SYSTEM_POLICY",
    fallbackLevel: 0,
  };
}
```

---

## 16. 调用失败后的降级

```ts
async function executeWithFallback(
  candidates: AiModelConfig[],
  execute: (model: AiModelConfig) => Promise<unknown>,
) {
  const errors = [];

  for (const model of candidates) {
    try {
      return await execute(model);
    } catch (error) {
      errors.push({
        modelId: model.id,
        error,
      });

      if (!isRetryableProviderError(error)) {
        throw error;
      }
    }
  }

  throw new AllAiModelsFailedError(errors);
}
```

可降级错误：

```text
429
502
503
504
连接超时
Provider 超时
Provider 暂时不可用
```

不可降级错误：

```text
用户输入非法
图片格式错误
Token 无权限
请求体过大
业务校验失败
```

---

## 17. 健康状态与熔断

模型状态：

```text
UNKNOWN
AVAILABLE
DEGRADED
UNAVAILABLE
```

建议规则：

```text
连续 3 次 Provider 失败
→ DEGRADED

短时间连续 5 次失败
→ UNAVAILABLE 5 分钟

冷却期结束
→ UNKNOWN

下一次探测成功
→ AVAILABLE
```

个人项目第一版可以只在内存中保存状态。

后续再持久化：

```text
近期错误率
平均延迟
限流次数
最后成功时间
```

---

## 18. 路由日志

每次调用记录：

```json
{
  "purpose": "VISION_ANALYZE",
  "requestedMode": "AUTO",
  "requestedModelId": null,
  "resolvedModelId": "gemini-vision",
  "resolvedBy": "SYSTEM_POLICY",
  "fallbackLevel": 0,
  "latencyMs": 2410,
  "status": "SUCCESS"
}
```

建议字段：

```text
用户 ID
用途
选择模式
实际模型
Provider
是否降级
耗时
状态
错误类型
Token 用量
```

禁止记录：

```text
完整 API Key
密码
Access Token
Refresh Token
敏感图片内容
```

---

## 19. Auto 动态评分的后续设计

第一版不实现复杂评分。

后续可引入：

```text
能力匹配
质量评分
近期成功率
平均延迟
当前负载
成本
用户偏好
```

示例：

```text
总分 =
能力匹配 × 35%
+
质量 × 25%
+
可用性 × 20%
+
延迟 × 10%
+
用户偏好 × 10%
```

但硬能力过滤必须始终先于评分。

---

## 20. 推荐落地阶段

### 第一阶段：基础可用

```text
系统视觉主模型
系统文本主模型
按用途固定路由
Auto / Manual
固定备用模型
```

验收：

```text
拍题走视觉模型
PDF 走视觉模型
辅导走文本模型
主模型失败后降级
```

---

### 第二阶段：用户模型

```text
用户新增模型
API Key 加密
用户模型参与 Auto
按用途保存偏好
辅导会话固定模型
```

---

### 第三阶段：智能路由

```text
健康检查
熔断
近期成功率
平均延迟
成本限制
动态评分
配额控制
```

---

## 21. 当前代码迁移建议

当前若存在：

```text
MobileAiConfig.isDefault
```

不再作为唯一默认机制。

迁移为：

```text
AiModelConfig
+
UserAiPreference
+
AiRoutePolicy
```

当前：

```text
拍题分析
→ getAIService()

AI 辅导
→ MobileAiConfig
```

应统一为：

```text
resolveAiModel(userId, purpose)
```

改造范围：

```text
POST /api/mobile/v1/analyze
POST /api/mobile/v1/question-banks/import
AI Tutor 接口
练习生成接口
答案解析接口
```

---

## 22. 验收标准

> 勾选规则：本对话完成 **App** 项；**Server** 项在 `bandu_web` 完成后勾选。  
> OpenSpec App 变更：`openspec/changes/ai-auto-model-routing-app/`。

### 系统模型

- [x] （Server）至少一个视觉模型。
- [x] （Server）至少一个文本模型。
- [x] （Server）系统模型由服务端管理。
- [x] （App）系统 API Key 不进入 App。

### 用户模型

- [x] （App）用户可新增、编辑、删除。
- [x] （Server）API Key 加密保存。
- [x] （App）App 只显示掩码。
- [x] （App）用户模型可以选择是否参与 Auto。

### Auto

- [x] （App）Auto 作为虚拟模型入口展示与透传。
- [x] （Server）Auto 作为虚拟模型入口解析。
- [x] （Server）按用途筛选。
- [x] （Server）按能力过滤。
- [x] （Server）主模型不可用时降级。
- [x] （Server）记录实际使用模型。
- [x] （App）展示实际使用模型与降级提示。
- [x] （App）手动模式显式透传所选模型与降级许可。
- [x] （Server）手动模式默认不偷偷切换。

### 多轮会话

- [x] （Server）新会话确定模型。
- [x] （Server）同一会话固定模型。
- [x] （App）用户主动切换偏好后提示仅影响新会话。
- [x] （Server）用户主动切换后更新模型。
- [x] （App）故障切换有明确提示。

### 安全

- [x] （App）不返回、存储或展示完整 API Key。
- [x] （Server）不返回完整 API Key。
- [x] （Server）不记录敏感密钥。
- [x] （Server）用户只能访问自己的模型配置。
- [x] （App）系统模型不可被普通用户删除。
- [x] （Server）系统模型不可被普通用户删除。

---

## 22.1 App / Server 任务划分（跟踪）

### App（本仓库 `bandu_app`，本对话范围）

- [x] 模型目录 UI（Auto / 系统 / 我的）
- [x] 按用途偏好 UI 与同步
- [x] 用户模型 CRUD（掩码 Key）
- [x] AI 请求透传 purpose + modelSelection
- [x] 展示 resolved 模型与降级提示
- [x] 迁移旧 `isDefault` 默认配置路径

### Server（`~/workspace/bandu_web`，不在本对话实现）

- [x] 系统模型与路由策略存储
- [x] API Key 加密与 models/preferences API
- [x] `resolveAiModel` + Provider 调用统一入口
- [x] 失败降级、健康状态与路由日志
- [x] 辅导会话模型固定

---

## 23. 最终定义

Bandu 的 Auto 定义为：

> 服务端按业务用途、模型能力、用户偏好、健康状态和优先级自动选择 AI 模型，并在可恢复故障时按配置降级到备用模型。

最终关系：

```text
Auto
=
用途路由
+
能力过滤
+
优先级
+
健康状态
+
失败降级
```

而不是：

```text
随机模型
单一默认模型
客户端直接决定 Provider
```