## Context

项目为 Next.js App Router，侧边栏由 `src/components/Sidebar.jsx` 渲染，收起状态由 `src/views/InteractiveMakerWorldPage.jsx` 的 `sidebarCollapsed` 控制；主内容区通过 `xl:ml-[72px]` / `xl:ml-[248px]` 与侧边栏宽度对应。当前侧边栏仅使用 `transition-[width] duration-200`，主内容区 margin 无过渡，侧边栏内文案/图标为条件渲染（无过渡），与 Makerworld 官网的连贯动画体验有差距。

## Goals / Non-Goals

**Goals:**
- 侧边栏宽度、主内容区左边距使用统一时长与缓动（如 250–300ms、ease-in-out），视觉上与 Makerworld 风格一致。
- 侧边栏内“仅展开时可见”的内容（Logo 旁文案、探索标题、footer）具备淡入淡出或短延迟，避免瞬间出现/消失。
- Toggle 按钮与动画节奏一致（可选：图标旋转或双态图标）。
- 主内容区左边距与侧边栏宽度同步过渡，无布局跳动。

**Non-Goals:**
- 不改变侧边栏信息架构或移动端导航。
- 不引入新依赖（仅用 CSS/Tailwind 与现有 React 状态）。

## Decisions

### 使用 CSS transition 统一宽度与 margin
侧边栏 `width` 与 main 的 `margin-left` 均通过 CSS `transition` 驱动，使用相同 duration 与 easing（如 `duration-300 ease-in-out`），保证两者同步变化。

**备选**：用 JS 动画库（如 framer-motion）。**不采纳**：增加依赖且本需求用 CSS 即可满足。

### 侧边栏内容可见性用 opacity + pointer-events，避免布局塌陷
展开时显示、收起时隐藏的块（Logo 旁文字、探索标题、footer）不采用 `display: none` 切换，而采用 `opacity` + `visibility`/`pointer-events` 与短 `transition-delay`，在宽度动画中段再淡入/淡出，避免内容突然出现或挤压。

**备选**：仅用 `display` 切换。**不采纳**：无法做淡入淡出，观感生硬。

### Toggle 按钮使用 CSS 旋转或保持现有图标
在保持现有 `IconMenuPanel` 的前提下，可为按钮增加 `transition-transform`，收起/展开时旋转 180° 以增强状态反馈；若设计上更接近 Makerworld 的双态图标，可后续替换图标组件，本阶段不强制。

**备选**：不碰 toggle 样式。**不采纳**：与“与动画节奏一致”的目标不符。

### 主内容区通过 Tailwind 的 transition 类过渡 margin
在 `InteractiveMakerWorldPage.jsx` 中为 main 的 `xl:ml-*` 容器增加 `transition-[margin] duration-* ease-in-out`，与侧边栏 `duration` 一致，实现联动。

## Risks / Trade-offs

- [Risk] 使用 `opacity` 保留 DOM 时，收起状态下可访问性（键盘/屏幕阅读器）可能仍聚焦到不可见内容。  
  **Mitigation**：对“仅展开可见”的块在收起时加 `aria-hidden="true"` 与 `inert`（或 `tabIndex=-1`）以配合 `pointer-events: none`。

- [Risk] 过渡时间过长会显得拖沓。  
  **Mitigation**：采用 250–300ms，与 Makerworld 相近；若需可后续通过 CSS 变量统一调节。

## Migration Plan

无数据或 API 变更。部署后为纯前端 UI 更新，可通过手动点击侧边栏收起/展开做回归；若有 E2E，可增加“展开→收起→再展开”的动画与布局断言。

## Open Questions

- 是否需要与 Makerworld 完全一致的精确时长/曲线（需在浏览器中实测其数值）？当前设计采用 250–300ms + ease-in-out 作为可调默认值。
