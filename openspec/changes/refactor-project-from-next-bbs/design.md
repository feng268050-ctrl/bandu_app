## Context

当前项目为 maker-world-clone（Next.js App Router，侧边栏、多视图、mock 数据）。参考仓库 next-bbs（`git@git.lasercyber.com:fullstack/next-bbs.git`）为同一组织内的另一项目，需先克隆、理解其用途与结构，再据此对当前项目进行有范围的重构。克隆与访问 next-bbs 可能依赖内网或 SSH 配置。

## Goals / Non-Goals

**Goals:**
- 在约定位置克隆 next-bbs，保证可重复、文档可引用。
- 产出「next-bbs 项目说明」文档：项目是做什么的、技术栈、目录与模块结构、关键约定（路由、状态、组件组织等），为后续重构提供唯一事实来源。
- 在理解 next-bbs 的基础上，对当前项目进行重构；重构范围在「理解文档」完成后确定（如目录对齐、组件拆分、状态/路由约定等），并确保现有功能不退化。

**Non-Goals:**
- 不将 next-bbs 直接替换当前项目；不改变当前项目的业务定位（如仍为 Makerworld 风格克隆）。
- 不承诺重构范围在提案阶段全部敲定；具体重构项在阅读 next-bbs 并编写说明文档后再细化到任务。

## Decisions

### 克隆位置与方式
- 克隆到当前工作区同级或父级目录（如 `e:\workspace\maker_world\next-bbs`），避免放入 maker-world-clone 仓库内造成嵌套与 git 冲突。若组织规范指定其他路径，以规范为准。
- 使用 `git clone git@git.lasercyber.com:fullstack/next-bbs.git`；若需认证，由执行人在本地配置 SSH 或凭据。

**备选**：克隆到 maker-world-clone 的 `vendor/` 或 `reference/`。**不采纳**：易与主仓库混合，且 .git 嵌套需额外处理。

### 文档产出形式与位置
- 「next-bbs 项目说明」以 Markdown 形式放在本变更目录下（如 `openspec/changes/refactor-project-from-next-bbs/next-bbs-overview.md`），或项目根目录的 `docs/`（若存在）。内容须包含：项目定位与主要功能、技术栈、顶层目录与关键模块、路由与数据流（若适用）、与当前项目可对齐的约定或模式。
- 文档完成后，基于该文档起草或更新「重构范围」清单（可在 design 或 tasks 中），再实施重构任务。

**备选**：仅口头或会议传递对 next-bbs 的理解。**不采纳**：需可追溯、可复用的书面说明以驱动重构。

### 重构范围后置
- 具体重构项（目录、组件、状态、路由等）不在设计阶段全部锁定；先完成克隆与 next-bbs 说明文档，再根据文档内容与当前项目差异列出重构任务，并按优先级实施，避免过早承诺范围。

**备选**：在提案中固定重构清单。**不采纳**：next-bbs 实际结构未知，固定清单易脱离实际。

## Risks / Trade-offs

- [Risk] 无法访问 `git.lasercyber.com` 或无 SSH 权限导致克隆失败。  
  **Mitigation**：任务中明确「由具备权限的人员在指定路径执行 clone」；若自动化环境无权限，将克隆与文档编写拆为可人工完成的步骤，并在文档中注明 next-bbs 版本/commit。

- [Risk] 重构范围过大导致周期长或引入回归。  
  **Mitigation**：重构分阶段、可验收；每阶段保持当前项目可运行；优先做「讲清楚 next-bbs」与高价值、低风险的对齐项。

- [Risk] next-bbs 与当前项目技术栈或目标差异大，可借鉴点有限。  
  **Mitigation**：文档中明确「可对齐的约定」与「不适用的部分」；重构仅采纳明确有益且可实施的项。

## Migration Plan

- 克隆与文档：无部署影响；文档纳入版本控制。
- 重构阶段：按任务分批提交；每批完成后做基础功能回归（如侧边栏、路由、主要页面）；无数据迁移除非设计明确涉及持久化变更。

## Open Questions

- 克隆路径的最终约定（工作区同级目录 vs 组织规范路径）需在执行前确认。
- 重构是否包含样式/主题、构建或 CI 配置，需在阅读 next-bbs 后根据文档再定。
