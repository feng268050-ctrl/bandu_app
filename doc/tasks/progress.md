# 伴读题集 Android App 实施进度

需求基线：[proposal.md](../proposal.md)

设计基线：[detailed-design.md](../detailed-design.md)

## 使用规则

- 一个模块只有在其任务文件中所有“任务”和“模块完成条件”均勾选后，才能在本文件标记完成。
- 一个任务应对应一次可独立审查的提交，任务中要求的验证必须通过后才能勾选。
- 开始任务前先确认其依赖模块已完成，或已提供可编译的稳定接口。
- 发现需求变化时，先修改需求和详细设计，再修改任务文件；不要直接扩展任务范围。
- 不实现账号、云同步、自动同步、打印、文件备份、GeoGebra、英文界面等首版外功能。

## 总体进度

| 指标 | 数量 |
| --- | ---: |
| 模块总数 | 22 |
| 已完成 | 19 |
| 进行中 | 3 |
| 未开始 | 0 |

## 第 1 批：工程与稳定模型

- [x] [`:app`](./app.md) - 根工程、App 外壳和导航（APP-001/002 完成）
- [x] [`:core:common`](./core-common.md) - 时间、UUID、Dispatcher、结果和日志
- [x] [`:core:model`](./core-model.md) - 稳定业务模型

批次完成标准：

- [x] `android/` 可执行 `./gradlew projects` 并显示全部 22 个模块。
- [x] Common 和 Model 测试通过，且不依赖 Feature 或基础设施实现。

## 第 2 批：领域接口与测试支撑

- [x] [`:domain`](./domain.md) - Repository 合约和 UseCase
- [x] [`:ai:api`](./ai-api.md) - AI 公共协议
- [x] [`:core:testing`](./core-testing.md) - Fake、Fixture 和架构测试

批次完成标准：

- [x] Domain 与 AI API 均可作为纯 JVM 模块测试。
- [x] 每类 Repository、AI 和 Transfer 均有可控 Fake。

## 第 3 批：本地基础设施

- [x] [`:core:designsystem`](./core-designsystem.md) - 主题和通用 UI
- [x] [`:core:storage`](./core-storage.md) - Room、DataStore、图片、Keystore、存储槽
- [x] [`:core:network`](./core-network.md) - HTTP、SSE 和端点安全

批次完成标准：

- [x] Room schema v1、迁移测试、FTS 和双槽切换通过。
- [x] Markdown/LaTeX 可离线安全渲染。
- [x] HTTP 私网策略和日志脱敏测试通过。

## 第 4 批：AI Provider

- [x] [`:ai:gemini`](./ai-gemini.md) - Gemini 适配
- [x] [`:ai:openai`](./ai-openai.md) - OpenAI-compatible 适配

批次完成标准：

- [x] 两个 Provider 均通过配置验证、图片分析、流式辅导、练习和批改测试。
- [x] API Key、图片和正文未出现在日志。

## 第 5 批：数据实现

- [x] [`:data`](./data.md) - Repository 实现和基础设施协调

批次完成标准：

- [x] 全部 Domain Repository 有可注入实现。
- [x] Room、AI、Keystore 和清除范围集成测试通过。

## 第 6 批：纯本地用户功能

- [x] [`:feature:home`](./feature-home.md) - 首页
- [x] [`:feature:library`](./feature-library.md) - 题集与错题
- [x] [`:feature:tags`](./feature-tags.md) - 标签
- [x] [`:feature:stats`](./feature-stats.md) - 统计

批次完成标准：

- [x] 无网络时四个 Feature 均可正常使用。
- [x] Feature 之间没有直接 Gradle 依赖。
- [x] 5000 条错题的列表和筛选性能达到设计门槛。

## 第 7 批：AI 用户功能

- [x] [`:feature:profile`](./feature-profile.md) - 资料、AI 设置和数据管理
- [ ] [`:feature:capture`](./feature-capture.md) - 拍照、分析和保存
- [x] [`:feature:tutor`](./feature-tutor.md) - 辅导、练习和批改

批次完成标准：

- [ ] AI 配置缺失后可完成配置并恢复 Capture/Tutor 原操作。
- [ ] 拍照和相册链路、流式停止、四档练习和三态批改通过。
- [ ] 清除学习数据与恢复出厂设置的保留范围正确。

## 第 8 批：设备协议与运行时

- [x] [`:transfer:protocol`](./transfer-protocol.md) - Protobuf、SRP、加密和状态机
- [ ] [`:transfer:runtime`](./transfer-runtime.md) - NSD、TCP、续传和原子导入
- [ ] [`:feature:devices`](./feature-devices.md) - 设备与迁移 UI

批次完成标准：

- [ ] 错误码、过期码、篡改、重放和身份变化均被拒绝。
- [ ] 40% 断网后只续传缺失分块。
- [ ] 迁移失败保留目标原数据，成功后源保留、目标 Key 清除。

## 最终验收

- [ ] [proposal.md](../proposal.md) 中全部 `AC-*` 验收项通过。
- [ ] 22 个模块的任务文件全部完成。
- [ ] Debug 与 Release 构建通过。
- [ ] 全部 JVM、Android 仪器、Compose、双设备和 Macrobenchmark 测试通过。
- [ ] 5000 条错题性能基准通过。
- [ ] Release APK 不包含遥测、远程日志、未使用权限或网页后端依赖。
- [ ] Git 变更不包含需求范围外功能。
