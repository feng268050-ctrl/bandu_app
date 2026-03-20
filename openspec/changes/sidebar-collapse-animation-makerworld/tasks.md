## 1. Sidebar width and timing

- [x] 1.1 In `Sidebar.jsx`, set sidebar root transition to a single duration and easing (e.g. `duration-300 ease-in-out`) for `width` so collapsed (72px) and expanded (248px) animate smoothly.
- [x] 1.2 Ensure no other inline or class overrides shorten or remove the sidebar width transition.

## 2. Main content margin transition

- [x] 2.1 In `InteractiveMakerWorldPage.jsx`, add `transition-[margin]` with the same duration and easing as the sidebar to the main content wrapper that uses `xl:ml-[72px]` / `xl:ml-[248px]`.
- [x] 2.2 Verify at xl breakpoint that main margin and sidebar width animate in sync when toggling.

## 3. Sidebar expand-only content fade

- [x] 3.1 In `Sidebar.jsx`, make “expand-only” blocks (logo text next to LogoMark, “Explore” title, footer) use opacity (and optional short delay) instead of conditional render; keep them in the DOM when collapsed but invisible (opacity-0, pointer-events-none).
- [x] 3.2 Apply transition (e.g. `transition-opacity duration-200`) and optional delay so content fades in/out in line with the width animation.
- [x] 3.3 When collapsed, set `aria-hidden="true"` and ensure these blocks are not focusable (e.g. `tabIndex={-1}` or inert) for accessibility.

## 4. Toggle button feedback

- [x] 4.1 Add a transition (e.g. `transition-transform`) to the sidebar toggle button in `Sidebar.jsx`.
- [x] 4.2 Implement visual feedback (e.g. rotate icon 180° when collapsed vs expanded) so the button state matches the sidebar animation rhythm.

## 5. Verification

- [x] 5.1 Manually test: expand → collapse → expand and confirm width, margin, content fade, and toggle feedback are smooth and consistent.
- [x] 5.2 Confirm keyboard focus does not land on hidden expand-only content when sidebar is collapsed.
