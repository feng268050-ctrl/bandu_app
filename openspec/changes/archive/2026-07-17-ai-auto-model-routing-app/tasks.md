## 1. Domain & Fake

- [x] 1.1 新增 `AiPurpose`、`AiModelKind`、`AiModelSummary`、`AiModelCatalog`、`AiPurposePreference` 等与设计文档对齐的领域模型，替换/旁路旧 `AiServiceConfig` 作为主路径
- [x] 1.2 定义 `AiModelRepository` / `AiPreferenceRepository` 合约与 Fake 实现，覆盖目录、CRUD、偏好读写
- [x] 1.3 为领域校验与 Fake 仓库补充单元测试

## 2. Conversion & Network

- [x] 2.1 实现 `/ai/models` 与 `/ai/preferences` 的 DTO、mapper（Key 仅掩码进领域层）
- [x] 2.2 实现 framework 层 API service + repository，并在 `framework_overrides` 注入
- [x] 2.3 服务端未就绪时保留 Fake 注入路径，便于 UI 独立开发

## 3. Catalog & Preferences UI

- [x] 3.1 改造 AI 设置页为模型目录：Auto / 系统模型 / 我的模型 + 添加入口（§12.1）
- [x] 3.2 增加按用途偏好 UI：拍题、PDF、辅导、出题、解析的 Auto/手动选择与相关开关（§12.2）
- [x] 3.3 自定义模型表单：能力开关、Key 新增必填/编辑留空不改、系统模型禁止删除（§12.3）
- [x] 3.4 Widget / 控制器测试：目录分区、偏好保存、删除守卫

## 4. Request Context & Resolved Display

- [x] 4.1 拍题分析、PDF 导入、辅导、练习生成请求携带 `purpose` 与 `modelSelection`
- [x] 4.2 解析服务端 `resolvedModel` / fallback 元数据并在相关 UI 展示（§12.4）；缺字段时安全降级
- [x] 4.3 多轮辅导：展示当前会话固定模型；用户切换偏好后有明确提示（App 侧展示，固定逻辑在服务端）

## 5. Migration & Acceptance

- [x] 5.1 迁移或隐藏旧 `isDefault` 唯一默认配置路径，避免与 Auto/系统模型冲突
- [x] 5.2 对照 `docs/bandu_ai_auto_model_routing_design.md` §22 中 App 相关项自测，并在文档中勾选已完成项
- [x] 5.3 更新 `.env.example` / 开发文档中与 AI 模型设置相关的说明（若需要）
