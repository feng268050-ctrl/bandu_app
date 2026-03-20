## Context

The project is a Next.js App Router UI that keeps sidebar selection and route synchronization inside `src/views/InteractiveMakerWorldPage.jsx`, while sidebar rendering lives in `src/components/Sidebar.jsx`. The requested change is small in scope but spans both navigation state and sidebar presentation, so documenting the implementation approach up front reduces ambiguity before coding.

Today, the sidebar collapsed state is stored as local UI state and toggled explicitly by the sidebar control. Internal module jumps already change active navigation state and route segments, so the safest implementation is to preserve that user-owned collapse state while navigation updates occur. The contest preview image change is a purely visual adjustment, but it needs a defined layout rule so the smaller asset does not leave broken spacing or overflow inside the sidebar surface.

## Goals / Non-Goals

**Goals:**
- Preserve the user's current sidebar collapsed or expanded state when internal sidebar modules trigger quick navigation.
- Reduce the sidebar contest activity preview image by 35% while maintaining aspect ratio.
- Keep surrounding sidebar spacing, copy, and alignment stable after the image size change.

**Non-Goals:**
- Redesign the sidebar information architecture or mobile navigation.
- Change external navigation behavior for links such as MakerLab or Parts Hub.
- Redesign contest content, copy, or data sourcing outside the sidebar preview surface.

## Decisions

### Preserve collapse state as explicit UI state
The implementation should continue treating `sidebarCollapsed` as user-owned state that only changes through the collapse toggle. Internal navigation handlers may update active nav state and routes, but they must not mutate the collapse flag as a side effect.

Alternative considered: recomputing collapsed state from the selected route or module. This was rejected because it would reintroduce layout jumps and make the sidebar feel unpredictable.

### Apply the preview image change through layout sizing, not transform scaling
The contest preview image should be resized through explicit layout dimensions or a constrained wrapper so the rendered footprint is 65% of the current sidebar treatment. This keeps the hit area, alignment, and image sharpness predictable.

Alternative considered: CSS `transform: scale(...)`. This was rejected because transforms can preserve the original layout box, create awkward empty space, and make the visual size differ from the interactive footprint.

### Scope the visual change to the sidebar contest preview surface only
The 35% reduction should apply only to the contest activity preview rendered in the sidebar context. Other contest images in main-content boards should remain unchanged unless they share the exact sidebar preview component.

Alternative considered: shrinking all contest preview imagery globally. This was rejected because it would create unrelated visual regressions outside the requested sidebar refinement.

## Risks / Trade-offs

- [Risk] The codebase may contain more than one sidebar-like contest preview surface. -> Mitigation: implementation should first identify the preview markup actually tied to the requested sidebar section and limit the change there.
- [Risk] A smaller image can leave unbalanced empty space in the contest card. -> Mitigation: adjust padding, alignment, and adjacent text layout together with the image size change.
- [Risk] Future route-sync logic could accidentally start mutating collapse state again. -> Mitigation: keep collapse writes centralized in the explicit toggle handler and avoid route-driven collapse logic.

## Migration Plan

No data or API migration is required. Rollout is a UI-only change that can be verified with desktop interaction checks for collapsed navigation and sidebar visual regression review.

## Open Questions

If the current implementation contains multiple candidate contest preview cards, the implementation phase should confirm which one the user means by "sidebar contest section" before broadening the styling change.
