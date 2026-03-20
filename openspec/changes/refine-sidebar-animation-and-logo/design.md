## Context

侧边栏已具备宽度与主内容 margin 的 transition（如 duration-300 ease-in-out），且部分「仅展开可见」内容通过 opacity 淡入淡出。用户反馈：收缩时仍有卡顿；展开时文字随宽度变化逐个排列出现，希望改为「展开完成后再一并出现」；收缩/展开按钮与侧边栏 Logo 需与参考网站（如 Makerworld）一比一模仿。

## Goals / Non-Goals

**Goals:**
- 消除收缩卡顿：通过合适的缓动（如 ease-out 或 cubic-bezier）与可能的 will-change/transform 优化，使宽度与 margin 过渡更顺滑。
- 展开完成后统一显示文字：仅展开时可见的块（Logo 旁文案、探索标题、footer）在**宽度动画结束**后再显示（如用 transitionend 或固定 delay ≥ 宽度动画时长），不随宽度变化逐个排列出现。
- 收缩/展开按钮：交互与视觉一比一参考站（点击反馈、图标形态/旋转等）。
- 侧边栏 Logo：样式与收起/展开态表现一比一参考站（可替换为参考站风格的 SVG 或图片）。

**Non-Goals:**
- 不改变侧边栏信息架构或路由逻辑。
- 不引入新前端依赖（仅 CSS/现有 React 状态，必要时静态资源）。

## Decisions

### 消除收缩卡顿：缓动与合成层
- 保持或微调 transition 时长（如 300ms），优先使用 `ease-out` 或与参考站一致的 cubic-bezier，避免 ease-in-out 在收尾时的轻微顿感。
- 对侧边栏根节点使用 `will-change: width`（或仅在动画期间临时添加）以促进合成层，减少重排导致的卡顿。若仍卡顿，可考虑用 transform: translateX 配合固定宽度占位替代直接改 width，需评估布局复杂度。

**备选**：仅延长 duration。**不采纳**：可能拖沓；先优化缓动与合成。

### 展开完成后统一显示文字：基于动画结束时机
- 仅展开时可见的内容在「展开动画结束」后再变为可见。实现方式二选一或组合：
  - **CSS**：对这些块设置 `transition-delay` 等于或略大于侧边栏宽度动画时长（如 300ms），且仅在展开态下 delay 生效；收缩时 delay 为 0 使内容立即隐藏，避免拖尾。
  - **JS**：监听侧边栏或 main 的 `transitionend`（或 300ms timeout），在展开完成后再移除「隐藏」类或设置可见，收缩时立即隐藏。
- 隐藏时仍使用 opacity-0 + pointer-events-none + invisible（及 aria-hidden），不占用布局空间（如 overflow-hidden + max-width: 0 或条件渲染在「收缩完成」后执行），避免展开过程中出现逐个排列的效果。

**备选**：保持当前淡入淡出与宽度同时进行。**不采纳**：与「展开后一并出现」需求不符。

### 按钮与 Logo 一比一模仿参考站
- **按钮**：对照参考站（如 Makerworld）的收缩/展开按钮：图标形状（单箭头/双箭头/面板图标）、旋转方向与角度、hover/active 状态、尺寸与位置。在现有 Sidebar 中替换或新增图标组件，并统一 transition 与状态类。
- **Logo**：对照参考站侧边栏的 Logo：收起态（仅图标或缩小版）与展开态（图标+文字或完整 Logo）。使用与参考站一致的 SVG 或图片资源，或使用现有组件库中相同/近似图标；若为外链图片，可下载到项目并引用，保证收起/展开两种布局下的表现一致。

**备选**：仅做样式微调。**不采纳**：用户明确要求一比一模仿。

## Risks / Trade-offs

- [Risk] 使用 transitionend 时需区分 width 与其它属性，避免多次触发。  
  **Mitigation**：只监听侧边栏根节点且 event.propertyName === 'width'，或使用单次 setTimeout(..., 宽度动画时长)。

- [Risk] 参考站 Logo/按钮的版权或商标。  
  **Mitigation**：本项目为克隆/模仿场景，若上线需确保符合参考站使用条款或仅作学习用途；实现时尽量用「视觉与交互一致」的等效实现而非直接复用其资源（若合规允许可复用）。

- [Risk] will-change 长期开启可能占内存。  
  **Mitigation**：仅在动画进行中短暂添加 will-change，动画结束后移除。

## Migration Plan

无数据迁移。部署后为前端 UI 更新；若 Logo 替换为图片/SVG，需部署相应静态资源。

## Open Questions

- 参考站（Makerworld）的收缩/展开按钮具体图标与 Logo 的最终视觉需在实现时对照其页面确认；若无法直接访问，可基于公开截图或描述实现「等效」交互与风格。
