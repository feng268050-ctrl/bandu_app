## 1. Data and API contract

- [x] 1.1 Define or confirm user model (id, name, email) and add DB schema/migration if the project has no user table yet
- [x] 1.2 Add indexes (or equivalent) on name and email for efficient partial, case-insensitive search
- [x] 1.3 Implement search logic: optional name and email params, partial case-insensitive match, AND when both provided, result cap and pagination (limit/offset or limit+cursor)
- [x] 1.4 Expose search as an API (e.g. GET /api/users/search?name=...&email=... or tRPC procedure) with response shape including id, name, email per user

## 2. Security and safety

- [x] 2.1 Restrict search endpoint to authenticated callers; return 401 when unauthenticated
- [x] 2.2 Optionally restrict to admin/role; document who can call search
- [x] 2.3 Add rate limiting for the search endpoint (e.g. per user or IP) and return 429 when exceeded
- [x] 2.4 Enforce minimum length for email fragment (e.g. 3+ characters) if applicable to reduce broad matches

## 3. Verification and docs

- [x] 3.1 Add tests for search scenarios: name only, email only, name+email, no matches, empty params, pagination, result cap
- [x] 3.2 Add tests for unauthorized and rate-limited requests
- [x] 3.3 Document the search API (params, response shape, auth, rate limits) in code or API docs

## 4. Optional UI (if in scope)

- [x] 4.1 Add a minimal UI (e.g. admin or settings page) with name/email inputs that calls the search API and displays results
- [x] 4.2 Ensure UI only loads or is reachable for authorized users (e.g. admin route guard)
