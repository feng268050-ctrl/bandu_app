# Reference page observation (Makerworld-style sidebar)

**Reference URL:** https://makerworld.com.cn/zh  
**Note:** When the reference page cannot be opened in this environment, the following is based on prior implementation alignment and common Makerworld-style sidebar patterns. User should confirm against the live reference.

## 1. Layout and structure (tasks 1.1, 1.2)

- **Block order:** Logo area (top) → Main navigation → Explore section (with heading) → Footer. Matches current `Sidebar.jsx` structure.
- **Collapsed width:** 72px (narrow strip; icon-only items).
- **Expanded width:** 248px.
- **Internal spacing:** Logo area pt-8, pb-5; header px-6 expanded / px-2 collapsed; nav px-4 expanded / px-2 collapsed; section title mt-8, mb-2; footer px-6 pb-6 pt-6, border-t. Nav items: gap-3, py-3 (main), py-2.5 (external); rounded-2xl.

## 2. Collapse/expand behavior (tasks 2.1, 2.2)

- **Toggle:** Single button in header (e.g. panel or chevron icon); one click toggles collapsed ↔ expanded.
- **Animation:** Width and main content margin transition together; duration ~300ms; easing ease-out to avoid stutter on collapse.
- **Expand-only content:** Logo text, "Explore" heading, footer appear only after the width expand animation completes (no staggered layout during expand); on collapse they hide immediately.
- **State persistence:** Collapsed state persisted (e.g. cookie) so reload/navigation restores it.
