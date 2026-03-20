/**
 * In-memory rate limiter for the user search API.
 * Limit: 60 requests per minute per client.
 * In production, use Redis or similar for multi-instance consistency.
 */

const WINDOW_MS = 60 * 1000
const MAX_REQUESTS = Number(process.env.RATE_LIMIT_MAX_REQUESTS) || 60

/** @type {Map<string, { count: number; resetAt: number }>} */
const store = new Map()

function getWindow(clientId) {
  const now = Date.now()
  let w = store.get(clientId)
  if (!w || now >= w.resetAt) {
    w = { count: 0, resetAt: now + WINDOW_MS }
    store.set(clientId, w)
  }
  return w
}

/**
 * @param {string} clientId - e.g. token slice or IP
 * @returns {{ allowed: boolean; retryAfterSeconds?: number }}
 */
export function checkRateLimit(clientId) {
  const key = String(clientId).slice(0, 64) || 'anonymous'
  const w = getWindow(key)
  w.count += 1
  if (w.count <= MAX_REQUESTS) {
    return { allowed: true }
  }
  const retryAfterSeconds = Math.ceil((w.resetAt - Date.now()) / 1000)
  return { allowed: false, retryAfterSeconds: Math.max(1, retryAfterSeconds) }
}

/** Reset rate limit store (for tests only). */
export function resetRateLimitForTesting() {
  store.clear()
}
