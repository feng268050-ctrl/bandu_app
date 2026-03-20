# User Search

Specification for searching users by name and email.

## ADDED Requirements

### Requirement: Search by name and email

The system SHALL provide a user search capability that accepts optional **name** and **email** query parameters. When both are provided, the system SHALL return only users that match both criteria (AND). When only one is provided, the system SHALL filter by that criterion only. Matching SHALL be partial and case-insensitive.

#### Scenario: Search by name only

- **WHEN** the caller requests user search with only a name parameter (e.g. `name=john`)
- **THEN** the system SHALL return all users whose name contains the given string (case-insensitive), and SHALL NOT require an email parameter

#### Scenario: Search by email only

- **WHEN** the caller requests user search with only an email parameter (e.g. `email=@example.com`)
- **THEN** the system SHALL return all users whose email contains the given string (case-insensitive), and SHALL NOT require a name parameter

#### Scenario: Search by name and email

- **WHEN** the caller requests user search with both name and email parameters
- **THEN** the system SHALL return only users that match both the name and the email criteria (AND)

#### Scenario: No matches

- **WHEN** the search criteria match no users
- **THEN** the system SHALL return an empty list and SHALL NOT return an error

#### Scenario: Empty or missing parameters

- **WHEN** the caller omits both name and email or supplies empty values
- **THEN** the system SHALL either return an empty list or apply a defined default (e.g. return no results or require at least one non-empty parameter); behavior SHALL be documented and consistent

### Requirement: Bounded and paginated results

The system SHALL limit the maximum number of users returned per search request and SHALL support pagination (e.g. limit/offset or limit/cursor) so that clients can page through results.

#### Scenario: Result cap

- **WHEN** the number of matching users exceeds the configured maximum (e.g. 50)
- **THEN** the system SHALL return at most that maximum and SHALL indicate that more results exist when pagination is used

#### Scenario: Pagination

- **WHEN** the caller requests a page (e.g. limit and offset or cursor)
- **THEN** the system SHALL return only that page of results and SHALL provide enough information for the client to request the next page if applicable

### Requirement: Consistent response shape

The system SHALL return each matching user in a consistent structure that SHALL include at least: user id, name, and email. Additional fields (e.g. profile or role) MAY be included as defined by the API contract.

#### Scenario: Response fields

- **WHEN** the search returns one or more users
- **THEN** each item in the response SHALL include id, name, and email, and SHALL use the same field names and types across all items

### Requirement: Access control and safety

The user search endpoint SHALL be restricted to authenticated callers. The system MAY further restrict access (e.g. to admins or specific roles). The system SHALL apply rate limiting or other safeguards to prevent abuse (e.g. scraping or enumeration).

#### Scenario: Unauthorized request

- **WHEN** an unauthenticated caller invokes user search
- **THEN** the system SHALL respond with an error (e.g. 401) and SHALL NOT return user data

#### Scenario: Rate limit

- **WHEN** a caller exceeds the defined rate limit for user search
- **THEN** the system SHALL respond with an error (e.g. 429) and SHALL NOT return user data for that request
