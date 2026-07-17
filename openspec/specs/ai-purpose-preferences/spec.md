## Requirements

### Requirement: Per-purpose Auto or manual preference
The Flutter app SHALL allow the user to set Auto or manual model selection per AI purpose (vision analyze, PDF import, tutor, question generate, answer explain) and sync preferences with the server.

#### Scenario: Default Auto preference
- **WHEN** the user has not overridden a purpose preference
- **THEN** the app treats that purpose as Auto when composing AI requests

#### Scenario: Manual model for tutor
- **WHEN** the user sets AI tutoring purpose to MANUAL with a selected model id
- **THEN** subsequent tutor requests include MANUAL mode and that model id

### Requirement: Auto participation and manual fallback flags
The Flutter app SHALL allow configuring whether user models may participate in Auto and whether manual mode allows fallback, and persist these flags via the preferences API.

#### Scenario: Update allow user models in Auto
- **WHEN** the user toggles allow-user-models-in-Auto for a purpose and saves
- **THEN** the app sends the updated preference to the server and reflects the saved state on reload
