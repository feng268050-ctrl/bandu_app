## ADDED Requirements

### Requirement: AI requests include purpose and model selection
The Flutter app SHALL attach an AI purpose and the user's model selection (Auto or manual model id) to AI feature requests such as capture analysis, PDF import, tutor, and practice generation.

#### Scenario: Vision analyze uses Auto
- **WHEN** the user runs image analysis with vision purpose preference set to Auto
- **THEN** the request payload includes purpose VISION_ANALYZE (or equivalent) and Auto selection

### Requirement: App displays resolved model after AI calls
When the server returns resolved model metadata, the Flutter app SHALL show the actual model used; if fallback occurred, the app SHALL show a clear degradation message.

#### Scenario: Show Auto resolution
- **WHEN** an Auto request completes with resolved model display name "Gemini Vision"
- **THEN** the UI shows that Auto selected that model rather than only the word Auto

#### Scenario: Show fallback notice
- **WHEN** the response indicates a fallback to a secondary model
- **THEN** the UI informs the user that the primary model was unavailable and a backup was used

#### Scenario: Missing resolved metadata
- **WHEN** the response omits resolved model fields
- **THEN** the app remains usable and does not crash; it may omit the detailed resolution line
