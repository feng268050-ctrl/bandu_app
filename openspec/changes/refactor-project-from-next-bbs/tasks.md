## 1. Clone next-bbs and document path

- [x] 1.1 Clone the repository `git@git.lasercyber.com:fullstack/next-bbs.git` to the agreed location (e.g. sibling directory of maker-world-clone or organization path); ensure SSH or access is configured if required.
- [x] 1.2 Document the clone path and the exact clone command (and any access notes) in this change directory (e.g. in a README or in design.md / next-bbs-overview.md) so others can reproduce.

## 2. Document what next-bbs is and how it is structured

- [x] 2.1 Create a next-bbs overview document (e.g. `next-bbs-overview.md` in this change dir or project `docs/`) that describes: what the project is for, main features, tech stack, and top-level directory/module structure.
- [x] 2.2 In the same document, describe key conventions (e.g. routing, state management, component organization, layout/sidebar) that are relevant for refactoring the current project.

## 3. Define refactoring scope from the overview

- [x] 3.1 Based on the next-bbs overview, list concrete refactoring items (e.g. align directory structure, split components, adopt routing or state conventions) and record them in tasks or design so that implementation is bounded.
- [x] 3.2 Prioritize or order refactoring items so that high-value, low-risk changes can be done first; keep existing functionality working after each step.

## 4. Implement refactoring

- [x] 4.1 Execute the refactoring tasks defined in section 3 (e.g. directory moves, component refactors, convention adoption) in the current project (maker-world-clone) without changing the project’s business purpose.
- [x] 4.2 After each logical batch, run the app and do a quick smoke check (sidebar, navigation, main views) to catch regressions early.

## 5. Verification

- [x] 5.1 Confirm that existing features (sidebar collapse/expand, navigation, main content views, locale) still work as before after refactoring.
- [x] 5.2 Confirm that the next-bbs overview document and clone path documentation are in place and sufficient for future reference.
