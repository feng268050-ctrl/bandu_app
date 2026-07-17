## Requirements

### Requirement: User can manage custom models
The Flutter app SHALL allow authenticated users to create, edit, and delete their own AI models through the mobile API, and MUST display API keys only as masks after save.

#### Scenario: Create user model
- **WHEN** the user submits a valid custom model form including API key and capability flags
- **THEN** the app posts the model to the server and shows the new entry with a masked key

#### Scenario: Edit without changing key
- **WHEN** the user edits a model and leaves the API key field blank
- **THEN** the app sends an update that does not replace the stored key

#### Scenario: Delete user model only
- **WHEN** the user deletes a user-owned model
- **THEN** the app calls delete and removes it from the local catalog view

#### Scenario: Cannot delete system model
- **WHEN** the user attempts to delete a system model
- **THEN** the app prevents the action and does not call delete for that id
