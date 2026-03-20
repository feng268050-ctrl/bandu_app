## Why

需要将参考仓库 `git@git.lasercyber.com:fullstack/next-bbs.git`（next-bbs）克隆到本地，先讲清楚该项目的用途与结构，再基于这一理解对当前项目（maker-world-clone）进行重构，使当前项目在架构、约定或功能组织上与之对齐或借鉴其设计。

## What Changes

- **克隆 next-bbs 仓库**：在合适位置克隆 `git@git.lasercyber.com:fullstack/next-bbs.git`，便于阅读与分析。
- **讲清楚 next-bbs 是做什么的**：产出文档，说明 next-bbs 的定位、主要功能、技术栈、目录与模块结构、关键约定，使团队与后续重构有统一认知。
- **基于 next-bbs 重构当前项目**：在理解 next-bbs 的前提下，对当前项目（maker-world-clone）进行重构；重构范围与方式由设计阶段确定（如目录结构、状态管理、路由与侧边栏约定、组件拆分等），并保持当前项目既有功能可用。

## Capabilities

### New Capabilities

- `next-bbs-understanding-and-refactor`: 克隆并理解 next-bbs 项目（文档化其用途与结构），并基于该理解对当前项目进行有范围的重构。

### Modified Capabilities

- （无）

## Impact

- **仓库与文档**：新增 next-bbs 克隆副本（或引用路径）；在 openspec 变更目录或项目根目录下新增「next-bbs 项目说明」文档。
- **当前项目**：可能涉及目录结构、组件划分、状态/路由约定、样式或构建配置等，以设计文档与任务为准；需保证现有功能不退化。
- **依赖与系统**：若有从 next-bbs 借鉴的依赖或配置，将体现在 package.json 或配置文件中；无后端或外部 API 契约变更除非设计明确纳入。
