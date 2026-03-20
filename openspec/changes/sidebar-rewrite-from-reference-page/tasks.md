## 1. Observe reference page (layout and structure)

- [x] 1.1 Open the reference page (e.g. https://makerworld.com.cn/zh), document the sidebar block order (logo, main nav, explore, footer), grouping, and visual hierarchy; optionally capture screenshots or measurements.
- [x] 1.2 Document collapsed width, expanded width, and key internal padding/gaps (e.g. logo area, nav items, footer) from the reference page.

## 2. Observe reference page (collapse/expand behavior)

- [x] 2.1 On the reference page, click the collapse/expand button multiple times; document the animation duration, easing (or perceived curve), and whether the main content area moves in sync.
- [x] 2.2 Document when expand-only content (e.g. logo text, section titles, footer) appears or disappears relative to the width animation (e.g. only after width finishes, or with a delay/fade).

## 3. Rewrite sidebar layout and structure

- [x] 3.1 In `Sidebar.jsx`, adjust or rewrite the layout structure so block order and grouping match the reference (logo area, main nav, explore section, footer).
- [x] 3.2 Align headings, section spacing, and visual hierarchy (e.g. active state, external link style) with the reference.

## 4. Rewrite sidebar dimensions and spacing

- [x] 4.1 Set sidebar collapsed and expanded widths to the reference values (or documented equivalents); ensure main content margin in `InteractiveMakerWorldPage.jsx` matches.
- [x] 4.2 Apply padding and gaps (logo, nav, footer) to match the reference so internal spacing is consistent.

## 5. Rewrite collapse/expand rules and animation

- [x] 5.1 Ensure a single toggle button controls collapse/expand and state persistence (e.g. cookie) matches the reference behavior; remove or add persistence only to align with reference.
- [x] 5.2 Implement collapse/expand animation (sidebar width + main content margin) with duration and easing that match the reference; ensure expand-only content visibility timing matches (e.g. show only after width animation ends if that is what the reference does).

## 6. Verification

- [x] 6.1 Manually compare the rewritten sidebar to the reference page: layout, dimensions, spacing, and collapse/expand animation and content timing.
- [x] 6.2 Confirm toggle behavior and state persistence match the reference (e.g. reload or navigate and check state).
