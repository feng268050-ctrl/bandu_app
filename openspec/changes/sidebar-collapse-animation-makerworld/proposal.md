## Why

参考 Makerworld 官网（https://makerworld.com.cn/zh）的侧边栏交互，当前项目侧边栏收起/展开仅有简单的 `transition-[width] duration-200`，缺少与主内容区联动、图标与文字过渡等完整动画体验。为本项目增加与之类似的侧边栏收缩/展开动画，可提升一致性与可用性。

## What Changes

- 侧边栏宽度在收起（72px）与展开（248px）之间使用与 Makerworld 风格一致的缓动与时长。
- 侧边栏内部内容（文案、图标、分隔等）在展开/收起时具备淡入淡出或延迟显示，避免突兀切换。
- 收缩/展开按钮（toggle）可选用旋转或状态图标切换，与动画节奏一致。
- 主内容区（main）的左边距（margin-left）随侧边栏宽度变化做同步过渡，避免布局跳动。

## Capabilities

### New Capabilities

- `sidebar-collapse-animation`: 侧边栏收起/展开时的宽度、内容可见性、主内容区边距及 toggle 按钮的动画行为与体验要求。

### Modified Capabilities

- （无：仅新增动画能力，不修改现有功能需求。）

## Impact

- **受影响代码**：`src/components/Sidebar.jsx`（侧边栏宽度、内部过渡、toggle 样式）、`src/views/InteractiveMakerWorldPage.jsx`（主内容区 `ml` 过渡）。
- **依赖**：仅用现有 Tailwind/CSS 与 React 状态，无新增运行时依赖。
- **系统**：仅前端 UI 表现，无 API 或后端变更。
