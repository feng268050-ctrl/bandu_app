## Requirements

### Requirement: App displays AI model catalog
The Flutter app SHALL present an AI model catalog with Auto, system models, and user models, without hardcoding provider vendor names.

#### Scenario: Catalog sections visible
- **WHEN** the user opens the AI model settings page after a successful catalog fetch
- **THEN** the app shows Auto as a recommended entry, a system models section, a user models section, and an action to add a user model

#### Scenario: System models are read-only in list
- **WHEN** the catalog includes system models
- **THEN** the app marks them as built-in and does not offer delete for those entries

### Requirement: App does not receive full system API keys
The Flutter app MUST NOT display or persist full API keys for system models.

#### Scenario: System model detail has no plaintext key
- **WHEN** the user views a system model
- **THEN** the app does not show a full API key field value from the server
