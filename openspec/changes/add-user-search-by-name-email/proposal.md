## Why

Users and admins need to find accounts by name or email (e.g. support, moderation, or management). Without search, locating a specific user is difficult at scale. Adding search by name and email addresses this need and improves operational efficiency.

## What Changes

- Add a user search capability that accepts **name** and **email** as search criteria.
- Expose search via API and/or UI so callers can query users by these fields.
- Return matching users with relevant fields (e.g. id, name, email, profile info) with safe handling of partial matches and empty results.

## Capabilities

### New Capabilities

- `user-search`: Search users by name and/or email; support partial match; return a list of matching users with consistent response shape; define access control and rate limits as needed.

### Modified Capabilities

- None.

## Impact

- New or extended API surface (e.g. REST or tRPC) for user search.
- Possible new UI (admin/settings) for searching users, depending on product scope.
- Data layer: queries against user/account tables (name, email); consider indexing for performance.
- No breaking changes to existing user-related APIs unless we explicitly refactor them.
