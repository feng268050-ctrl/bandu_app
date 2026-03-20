## 1. Sidebar navigation state

- [x] 1.1 Audit the internal sidebar navigation flow in `src/views/InteractiveMakerWorldPage.jsx` and ensure `sidebarCollapsed` is only changed by the explicit collapse toggle.
- [x] 1.2 Update main and explore sidebar quick-jump handlers so internal navigation preserves the current collapsed or expanded sidebar state.

## 2. Sidebar contest preview layout

- [x] 2.1 Identify the sidebar contest preview surface and reduce its activity preview image to 65% of the current displayed size while preserving aspect ratio.
- [x] 2.2 Adjust surrounding spacing and alignment so the reduced preview image does not clip content or expand the sidebar layout in either locale.

## 3. Verification

- [x] 3.1 Verify that collapsed and expanded sidebar states remain unchanged after internal module jumps across the supported sidebar destinations.
- [x] 3.2 Run the relevant project validation step and perform a desktop visual check of the compact contest preview layout for overflow or alignment regressions.
