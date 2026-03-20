## Context

Maker World Clone is a Next.js App Router application (zh/en, community, contests, laser-cut-models). User search by name and email is a new capability for support, moderation, or admin workflows. There are no existing user-search APIs or specs; this is a greenfield feature. The project may use a database (e.g. Neon Postgres) or external auth/data services; the design assumes a data layer that stores user identity (name, email) and can be queried.

## Goals / Non-Goals

**Goals:**

- Provide a single, consistent way to search users by name and/or email (partial match).
- Return a bounded list of matching users with stable response shape (id, name, email, and any agreed profile fields).
- Keep the contract API-first so UI (e.g. admin panel) or other clients can consume it later.
- Design for safe behavior: no sensitive over-exposure, optional auth/rate limits as required.

**Non-Goals:**

- Full-text search across all user attributes or arbitrary filters in this change.
- Changing existing auth or registration flows.
- Implementing a full admin dashboard; only the search capability is in scope (UI can be added later).

## Decisions

1. **Search interface (API)**  
   - **Choice**: Add a dedicated search endpoint (e.g. `GET /api/users/search?name=...&email=...` or equivalent in your stack, e.g. tRPC procedure).  
   - **Rationale**: Keeps search explicit, easy to secure and document.  
   - **Alternatives**: Extending a generic "list users" with filters was considered; a dedicated search endpoint is clearer and easier to scope and rate-limit.

2. **Matching semantics**  
   - **Choice**: Partial, case-insensitive match on name and email; both params optional; if both provided, combine with AND (user must match name and email).  
   - **Rationale**: Matches common expectations (e.g. "john" finds "John Doe") and avoids leaking full emails until results are narrowed.  
   - **Alternatives**: Exact-only match would be too strict; regex or advanced operators deferred to keep v1 simple.

3. **Result set and pagination**  
   - **Choice**: Cap results (e.g. max 50) and support a simple limit/offset or limit+cursor for pagination to avoid large responses.  
   - **Rationale**: Prevents abuse and keeps response size bounded.  
   - **Alternatives**: Unbounded list was rejected for performance and safety.

4. **Authorization and rate limiting**  
   - **Choice**: Restrict search to authenticated users (and optionally to admin/role) and apply a reasonable rate limit (e.g. per user or per IP).  
   - **Rationale**: User search can be sensitive; limiting who can call it and how often reduces abuse and data scraping.  
   - **Alternatives**: Public search was considered and rejected for privacy; exact role model can be decided in implementation.

5. **Data source**  
   - **Choice**: Implement search against the existing user/account store (DB or service). Add indexes on name and email (or normalized columns) for performance.  
   - **Rationale**: Reuses current source of truth; indexes keep queries fast.  
   - **Alternatives**: Separate search index (e.g. Elasticsearch) deferred until scale justifies it.

## Risks / Trade-offs

- **Risk**: Partial match on email could expose many accounts (e.g. "gmail" matching many users).  
  **Mitigation**: Require a minimum length for email fragment (e.g. 3+ characters), cap results, and restrict endpoint to authorized/role-based callers.

- **Risk**: No existing user store or schema.  
  **Mitigation**: Implementation tasks will define or assume a minimal user model (id, name, email) and add migration/schema if needed.

- **Trade-off**: AND semantics when both name and email are provided may reduce result count; acceptable for v1; OR or advanced filters can be a follow-up.

## Migration Plan

- Deploy as additive change: new route/handler and optional UI. No changes to existing user read/write paths required for minimal rollout.
- Rollback: remove or feature-flag the search endpoint and revert any new indexes if necessary; no data migration required for search-only.

## Open Questions

- Exact auth model (e.g. admin-only vs. any authenticated user) to be confirmed in implementation.
- Whether to return additional profile fields in search results and which ones; can be narrowed in tasks/specs.
