## ADDED Requirements

### Requirement: Sidebar layout and structure match the reference page

The sidebar SHALL follow the same layout and structure as the reference page (e.g. Makerworld): block order (e.g. logo area, main nav, explore section, footer), grouping of items, and visual hierarchy SHALL match the reference so that the sidebar reads and behaves like the reference.

#### Scenario: Block order matches reference

- **WHEN** the user views the sidebar in expanded state
- **THEN** the order and grouping of blocks (logo, main navigation, explore section, footer) SHALL match the reference page

#### Scenario: Visual hierarchy matches reference

- **WHEN** the user compares the sidebar to the reference page
- **THEN** headings, spacing between sections, and emphasis (e.g. active state, external links) SHALL align with the reference

---

### Requirement: Sidebar dimensions and spacing match the reference page

The sidebar SHALL use the same dimensions and spacing as the reference page: collapsed width, expanded width, internal padding, and item spacing SHALL match (or be within a defined tolerance of) the reference so that the layout is visually consistent.

#### Scenario: Collapsed and expanded widths match reference

- **WHEN** the sidebar is collapsed or expanded
- **THEN** the sidebar width SHALL match the reference page’s collapsed and expanded widths (or documented equivalents)

#### Scenario: Internal spacing matches reference

- **WHEN** the user compares padding and gaps inside the sidebar to the reference
- **THEN** padding around logo, nav items, and footer SHALL match the reference (or documented equivalents)

---

### Requirement: Collapse/expand rules match the reference page button behavior

The rules for when the sidebar is collapsed or expanded SHALL match the reference page: a single toggle button SHALL switch between collapsed and expanded; the state SHALL persist across navigation or reload if the reference does so (e.g. cookie or local storage); no automatic collapse/expand SHALL be introduced unless the reference exhibits it.

#### Scenario: Toggle switches state

- **WHEN** the user clicks the collapse/expand button on the sidebar
- **THEN** the sidebar SHALL switch from expanded to collapsed or from collapsed to expanded, consistent with the reference button behavior

#### Scenario: State persistence matches reference

- **WHEN** the user collapses or expands the sidebar and then navigates or reloads (if applicable on the reference)
- **THEN** the sidebar SHALL restore the same collapsed/expanded state as the reference page would (e.g. persisted via cookie or not), so behavior is aligned

---

### Requirement: Collapse/expand animation matches the reference page

The collapse/expand animation SHALL match the reference page: duration, easing, and the timing of when “expand-only” content (e.g. logo text, section titles, footer) appears or disappears SHALL be aligned with what is observed when clicking the reference page’s collapse/expand button.

#### Scenario: Width and main content animation match reference

- **WHEN** the user toggles the sidebar
- **THEN** the sidebar width and main content area offset SHALL animate with the same duration and easing as observed on the reference page

#### Scenario: Expand-only content timing matches reference

- **WHEN** the user expands or collapses the sidebar
- **THEN** the visibility of expand-only content (e.g. logo text, “Explore” title, footer) SHALL change in the same way as on the reference (e.g. only after width animation ends, or with the same delay/fade), so that the animation feels the same as the reference
