## Why

The current sidebar interaction does not clearly preserve the user's chosen collapsed state when they use module shortcuts for quick navigation. That creates avoidable layout changes and slows down repeat navigation.

The contest activity preview inside the sidebar also occupies too much visual weight for a compact navigation surface. Reducing the preview image size now will keep the sidebar focused on navigation while still exposing contest content.

## What Changes

- Keep the sidebar in its current collapsed state when users click sidebar modules to jump between internal sections.
- Update the sidebar contest section so its activity preview image renders at 65% of its current size.
- Adjust sidebar spacing and alignment around the contest preview so the reduced image size still reads cleanly in desktop layouts.

## Capabilities

### New Capabilities
- `sidebar-collapsed-navigation`: Preserve explicit sidebar collapsed state during internal module navigation.
- `sidebar-contest-preview-layout`: Present contest activity previews in a more compact sidebar layout with reduced image sizing.

### Modified Capabilities
- None.

## Impact

- Affected code will likely include sidebar state management in `src/views/InteractiveMakerWorldPage.jsx`.
- Sidebar rendering and contest preview layout changes will likely be implemented in `src/components/Sidebar.jsx` and related sidebar data/render helpers.
- No API or dependency changes are expected.
