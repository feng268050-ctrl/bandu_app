## ADDED Requirements

### Requirement: Internal sidebar navigation preserves the current collapse state
The system SHALL preserve the user's current sidebar collapsed or expanded state when an internal sidebar module is selected and the app navigates to a different internal section.

#### Scenario: Collapsed main navigation stays collapsed
- **WHEN** the user collapses the sidebar and selects an internal main navigation module from the sidebar
- **THEN** the destination section SHALL load with the sidebar still collapsed

#### Scenario: Collapsed explore navigation stays collapsed
- **WHEN** the user collapses the sidebar and selects an internal explore navigation module from the sidebar
- **THEN** the destination section SHALL load with the sidebar still collapsed

#### Scenario: Expanded navigation stays expanded
- **WHEN** the user leaves the sidebar expanded and selects an internal sidebar module
- **THEN** the destination section SHALL load with the sidebar still expanded
