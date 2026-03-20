## ADDED Requirements

### Requirement: Sidebar collapse animation is smooth with no stutter

The sidebar width and main content margin SHALL transition without perceptible stutter when collapsing. The implementation SHALL use an appropriate easing (e.g. ease-out or reference-site-aligned cubic-bezier) and MAY use compositing hints (e.g. will-change) only during the animation to keep the transition smooth.

#### Scenario: User collapses the sidebar

- **WHEN** the user clicks to collapse the sidebar
- **THEN** the sidebar width and main content margin SHALL animate to the collapsed state without visible stutter or frame drops

#### Scenario: User expands the sidebar

- **WHEN** the user clicks to expand the sidebar
- **THEN** the sidebar width and main content margin SHALL animate to the expanded state smoothly

---

### Requirement: Expand-only content appears only after expand animation completes

Content that is visible only when the sidebar is expanded (logo text, "Explore" title, footer) SHALL become visible only after the width expand animation has finished, not during the animation. Content SHALL NOT appear to arrange one-by-one as the width increases.

#### Scenario: Content appears after expand completes

- **WHEN** the sidebar transitions from collapsed to expanded
- **THEN** expand-only content SHALL become visible only after the width transition has completed (e.g. via delay equal to or greater than the width duration, or transitionend)

#### Scenario: No staggered appearance during expand

- **WHEN** the sidebar is expanding
- **THEN** expand-only content SHALL NOT visibly appear or arrange item-by-item while the width is still animating

#### Scenario: Content hides immediately on collapse

- **WHEN** the user collapses the sidebar
- **THEN** expand-only content SHALL be hidden immediately or with minimal delay so that it does not linger during collapse

---

### Requirement: Collapse/expand button matches reference site behavior and appearance

The sidebar collapse/expand toggle button SHALL match the reference site (e.g. Makerworld) in interaction and appearance: icon shape, rotation or state change, hover/active feedback, size and placement.

#### Scenario: Button matches reference interaction

- **WHEN** the user hovers or clicks the toggle button
- **THEN** the button SHALL provide the same visual and interaction feedback as the reference site

#### Scenario: Button icon state matches sidebar state

- **WHEN** the sidebar is collapsed or expanded
- **THEN** the toggle button icon SHALL reflect the same state (e.g. direction or shape) as on the reference site

---

### Requirement: Sidebar logo matches reference site in both collapsed and expanded states

The sidebar logo (icon and optional text) SHALL match the reference site in style and behavior in both collapsed and expanded states: visual style, size, and layout in each state.

#### Scenario: Logo in collapsed state matches reference

- **WHEN** the sidebar is collapsed
- **THEN** the logo SHALL be displayed in a form that matches the reference site (e.g. icon-only or reduced form)

#### Scenario: Logo in expanded state matches reference

- **WHEN** the sidebar is expanded
- **THEN** the logo SHALL be displayed in a form that matches the reference site (e.g. full logo or icon plus text)
