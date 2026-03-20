/**
 * User model and in-memory store for user search.
 * In production, replace with a database (e.g. Neon Postgres) and add indexes on name and email.
 *
 * @typedef {{ id: string; name: string; email: string }} User
 */

export const DEFAULT_SEARCH_LIMIT = 20
export const MAX_SEARCH_LIMIT = 50
export const MIN_EMAIL_FRAGMENT_LENGTH = 3

/** @type {User[]} */
const users = [
  { id: '1', name: 'Alice Chen', email: 'alice@example.com' },
  { id: '2', name: 'Bob Smith', email: 'bob.smith@example.com' },
  { id: '3', name: 'Carol Johnson', email: 'carol.j@example.com' },
  { id: '4', name: 'David Lee', email: 'david.lee@makerworld.com' },
  { id: '5', name: 'Eve Wilson', email: 'eve@test.org' },
  { id: '6', name: 'Frank Brown', email: 'frank.brown@example.com' },
  { id: '7', name: 'Grace Zhang', email: 'grace.zhang@example.com' },
  { id: '8', name: 'Henry Davis', email: 'henry@example.com' },
  { id: '9', name: 'Ivy Martinez', email: 'ivy.martinez@makerworld.com' },
  { id: '10', name: 'Jack Taylor', email: 'jack.t@example.com' },
]

/**
 * Search users by optional name and/or email (partial, case-insensitive).
 * When both are provided, only users matching both criteria are returned (AND).
 *
 * @param {{ name?: string; email?: string; limit?: number; offset?: number }} opts
 * @returns {{ users: User[]; total: number; hasMore: boolean }}
 */
export function searchUsers({ name, email, limit = DEFAULT_SEARCH_LIMIT, offset = 0 }) {
  const safeLimit = Math.min(Math.max(1, Math.floor(Number(limit)) || DEFAULT_SEARCH_LIMIT), MAX_SEARCH_LIMIT)
  const safeOffset = Math.max(0, Math.floor(Number(offset)) || 0)

  const nameNorm = typeof name === 'string' ? name.trim().toLowerCase() : ''
  const emailNorm = typeof email === 'string' ? email.trim().toLowerCase() : ''

  const hasName = nameNorm.length > 0
  const hasEmail = emailNorm.length > 0
  if (!hasName && !hasEmail) {
    return { users: [], total: 0, hasMore: false }
  }

  if (hasEmail && emailNorm.length < MIN_EMAIL_FRAGMENT_LENGTH) {
    return { users: [], total: 0, hasMore: false }
  }

  const filtered = users.filter((u) => {
    const matchName = !hasName || (u.name && u.name.toLowerCase().includes(nameNorm))
    const matchEmail = !hasEmail || (u.email && u.email.toLowerCase().includes(emailNorm))
    return matchName && matchEmail
  })

  const total = filtered.length
  const usersPage = filtered.slice(safeOffset, safeOffset + safeLimit)
  const hasMore = safeOffset + usersPage.length < total

  return { users: usersPage, total, hasMore }
}
