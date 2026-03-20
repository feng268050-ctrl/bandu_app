## ADDED Requirements

### Requirement: Sidebar width animates between collapsed and expanded

The sidebar SHALL animate its width between 72px (collapsed) and 248px (expanded) using a single transition with consistent duration and easing (e.g. 250–300ms, ease-in-out), so that the change is smooth and visually aligned with the main content margin.

#### Scenario: User toggles from expanded to collapsed

- **WHEN** the user clicks the sidebar collapse/expand button while the sidebar is expanded
- **THEN** the sidebar width SHALL transition from 248px to 72px over the configured duration with the configured easing

#### Scenario: User toggles from collapsed to expanded

- **WHEN** the user clicks the sidebar collapse/expand button while the sidebar is collapsed
- **THEN** the sidebar width SHALL transition from 72px to 248px over the configured duration with the configured easing

---

### Requirement: Main content margin animates in sync with sidebar width

The main content area SHALL animate its left margin (xl breakpoint) in sync with the sidebar width (72px when collapsed, 248px when expanded), using the same duration and easing as the sidebar, so that no layout jump occurs.

#### Scenario: Main margin transitions on collapse

- **WHEN** the sidebar is toggled from expanded to collapsed
- **THEN** the main content left margin SHALL transition from 248px to 72px with the same duration and easing as the sidebar width

#### Scenario: Main margin transitions on expand

- **WHEN** the sidebar is toggled from collapsed to expanded
- **THEN** the main content left margin SHALL transition from 72px to 248px with the same duration and easing as the sidebar width

---

### Requirement: Sidebar content fades for expand-only visible blocks

Blocks that are visible only when the sidebar is expanded (e.g. logo text, “Explore” title, footer) SHALL use opacity (and optional short delay) transitions so that they fade in when expanding and fade out when collapsing, instead of appearing or disappearing instantly.

#### Scenario: Expand-only content fades in on expand

- **WHEN** the sidebar transitions from collapsed to expanded
- **THEN** the expand-only content (logo text, section title, footer) SHALL become visible via an opacity transition rather than an instant show

#### Scenario: Expand-only content fades out on collapse

- **WHEN** the sidebar transitions from expanded to collapsed
- **THEN** the expand-only content SHALL become hidden via an opacity transition rather than an instant hide

---

### Requirement: Toggle button reflects animation rhythm

The sidebar collapse/expand toggle button SHALL reflect the same animation rhythm (e.g. via rotation or state icon) so that its feedback is consistent with the sidebar and main content transitions.

#### Scenario: Toggle has transition feedback on click

- **WHEN** the user clicks the toggle button
- **THEN** the button SHALL provide visual feedback (e.g. icon rotation or state change) that aligns with the sidebar animation duration

---

### Requirement: Collapsed state preserves accessibility for hidden content

When the sidebar is collapsed, expand-only content SHALL be hidden from assistive technologies and keyboard focus (e.g. aria-hidden and non-focusable) so that users do not focus or hear content that is not visibly available.

#### Scenario: Hidden content is not focusable when collapsed

- **WHEN** the sidebar is collapsed and the user navigates by keyboard
- **THEN** focus SHALL NOT land on expand-only content that is visually hidden
