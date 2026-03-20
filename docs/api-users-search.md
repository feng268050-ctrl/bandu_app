# User Search API

## Endpoint

`GET /api/users/search`

## Query parameters

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| `name`    | string | No       | Partial, case-insensitive match on user name. |
| `email`   | string | No       | Partial, case-insensitive match on email (minimum 3 characters). |
| `limit`   | number | No       | Max results per page (default 20, max 50). |
| `offset`  | number | No       | Pagination offset (default 0). |

- At least one of `name` or `email` must be provided (or both). If both are provided, results match both criteria (AND).
- If only `email` is provided and is shorter than 3 characters, the response is an empty list.

## Authentication

- **Required.** Send either:
  - `Authorization: Bearer <token>`, or
  - `X-API-Key: <key>`
- If missing or empty: **401 Unauthorized**.

## Rate limiting

- **60 requests per minute** per client (derived from token or IP).
- When exceeded: **429 Too Many Requests** with `Retry-After` header.

## Response shape

**200 OK**

```json
{
  "users": [
    { "id": "string", "name": "string", "email": "string" }
  ],
  "total": 0,
  "hasMore": false
}
```

- `users`: Array of matching users (id, name, email).
- `total`: Total number of matches (before pagination).
- `hasMore`: `true` if more results are available (use `offset` + `limit` for next page).

## Example

```bash
curl -H "Authorization: Bearer your-token" \
  "http://localhost:3000/api/users/search?name=alice&limit=10&offset=0"
```

## Testing

- **Unit tests** (search logic): `npm run test`
- **API integration** (401, 200): Start dev server (`npm run dev`), then run `npm run test:api`
- **Manual** (429, curl): Use the curl example above; omit the header to verify 401.
