## 1. Smooth collapse animation (no stutter)

- [x] 1.1 In `Sidebar.jsx` and main wrapper, switch to an easing that avoids stutter on collapse (e.g. `ease-out` or a cubic-bezier aligned with the reference site); keep or slightly tune duration (e.g. 300ms).
- [x] 1.2 Optionally add `will-change: width` only during the sidebar width transition (e.g. set on toggle start, remove in transitionend or after a short timeout) to improve compositing; ensure it is cleared after animation to avoid memory cost.

## 2. Show expand-only content only after expand completes

- [x] 2.1 Change expand-only blocks (logo text, Explore title, footer) so they become visible only after the width expand animation finishes: use CSS `transition-delay` equal to (or slightly greater than) the sidebar width duration when expanding, and no delay when collapsing so content hides immediately.
- [x] 2.2 If needed, implement JS-based timing: on expand, keep content hidden until `transitionend` (propertyName === 'width') or a timeout equal to the width duration; on collapse, hide content immediately. Ensure content does not appear to arrange one-by-one during expand.

## 3. Toggle button matches reference site

- [x] 3.1 Align the collapse/expand button with the reference site (e.g. Makerworld): icon shape (single/double arrow or panel icon), size, and position.
- [x] 3.2 Add or adjust hover/active styles and icon state (e.g. rotation or swap icon) so interaction and visual feedback match the reference site.

## 4. Sidebar logo matches reference site

- [x] 4.1 In collapsed state, show the logo in the same way as the reference site (e.g. icon-only or reduced); replace or adjust `LogoMark` and layout as needed.
- [x] 4.2 In expanded state, show the full logo (e.g. icon + "Laser" / "Cyber" or reference-site equivalent); use matching assets (SVG/image) or equivalent styling.

## 5. Verification

- [x] 5.1 Manually test: collapse and expand multiple times; confirm no stutter on collapse and that expand-only content appears only after the width animation completes (no one-by-one arrangement).
- [x] 5.2 Compare toggle button and sidebar logo with the reference site and confirm behavior and appearance match.
