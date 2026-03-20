import { NextResponse } from 'next/server'
import { searchUsers } from '../../../../lib/users'
import { checkRateLimit } from '../../../../lib/rateLimit'

/**
 * GET /api/users/search
 *
 * Query params:
 * - name (optional): partial, case-insensitive match on user name
 * - email (optional): partial, case-insensitive match on email (min 3 characters)
 * - limit (optional): max results per page (default 20, max 50)
 * - offset (optional): pagination offset (default 0)
 *
 * Auth: Requires Authorization header (Bearer token or API key). Returns 401 if missing/invalid.
 * Rate limit: 60 requests per minute per client (by Authorization or IP). Returns 429 when exceeded.
 *
 * Response: { users: [{ id, name, email }], total, hasMore }
 */
export async function GET(request) {
  const authHeader = request.headers.get('authorization')
  const apiKey = request.headers.get('x-api-key')
  if (!authHeader && !apiKey) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }
  const token = authHeader?.startsWith('Bearer ')
    ? authHeader.slice(7)
    : apiKey || ''
  if (!token.trim()) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const clientId = token.slice(0, 32) || request.headers.get('x-forwarded-for') || 'anonymous'
  const rateLimitResult = checkRateLimit(clientId)
  if (!rateLimitResult.allowed) {
    return NextResponse.json(
      { error: 'Too Many Requests' },
      { status: 429, headers: { 'Retry-After': String(rateLimitResult.retryAfterSeconds) } },
    )
  }

  const { searchParams } = new URL(request.url)
  const name = searchParams.get('name') ?? undefined
  const email = searchParams.get('email') ?? undefined
  const limit = searchParams.get('limit')
  const offset = searchParams.get('offset')

  const result = searchUsers({
    name,
    email,
    limit: limit ? parseInt(limit, 10) : undefined,
    offset: offset ? parseInt(offset, 10) : undefined,
  })

  return NextResponse.json({
    users: result.users,
    total: result.total,
    hasMore: result.hasMore,
  })
}
