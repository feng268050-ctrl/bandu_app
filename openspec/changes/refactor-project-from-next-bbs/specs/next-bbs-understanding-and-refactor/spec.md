## ADDED Requirements

### Requirement: next-bbs repository is cloned in an agreed location

The next-bbs repository (git@git.lasercyber.com:fullstack/next-bbs.git) SHALL be cloned to an agreed location (e.g. sibling directory of the current project or organization-standard path) so that the codebase can be read and referenced for documentation and refactoring.

#### Scenario: Clone is available for analysis

- **WHEN** a maintainer or automation runs the clone step with valid access
- **THEN** the next-bbs repository SHALL be present at the agreed path and SHALL be usable for reading source and structure

#### Scenario: Clone path is documented

- **WHEN** the change is implemented
- **THEN** the clone path and how to perform the clone (e.g. git clone command and any access requirements) SHALL be documented so that others can reproduce or update the reference

---

### Requirement: next-bbs project purpose and structure are documented

A written overview of the next-bbs project SHALL be produced that explains what the project is for, its main features, tech stack, directory and module structure, and key conventions (e.g. routing, state, component organization) so that the team shares one source of truth and refactoring can be scoped from it.

#### Scenario: Document describes what next-bbs does

- **WHEN** a reader opens the next-bbs overview document
- **THEN** they SHALL be able to understand the project’s purpose and main features in a few minutes

#### Scenario: Document describes structure and conventions

- **WHEN** a reader uses the document to plan or perform refactoring of the current project
- **THEN** the document SHALL describe enough of next-bbs’s directory layout, key modules, and conventions (e.g. routing, state, components) to support informed refactoring decisions

---

### Requirement: Current project is refactored based on next-bbs understanding

The current project (maker-world-clone) SHALL be refactored based on the documented understanding of next-bbs. The refactoring scope SHALL be defined after the overview document exists (e.g. directory structure, component split, state or routing conventions) and SHALL preserve existing functionality without regression.

#### Scenario: Refactoring scope is defined from the overview

- **WHEN** the next-bbs overview document is complete
- **THEN** a refactoring scope (list of concrete changes or areas) SHALL be defined and tracked (e.g. in design or tasks) so that implementation is bounded and verifiable

#### Scenario: Refactoring preserves existing behavior

- **WHEN** refactoring tasks are completed
- **THEN** existing features (e.g. sidebar, navigation, main views) SHALL continue to work as before unless a change explicitly alters behavior and is accepted as such
