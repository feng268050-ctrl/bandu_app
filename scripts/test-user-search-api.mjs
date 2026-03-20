#!/usr/bin/env node
/**
 * Integration test for GET /api/users/search.
 * Run with dev server up: npm run dev (in another terminal), then: node scripts/test-user-search-api.mjs
 */
const BASE = process.env.API_BASE || 'http://localhost:3000'

async function test(name, fn) {
  try {
    await fn()
    console.log(`✔ ${name}`)
  } catch (e) {
    console.error(`✖ ${name}:`, e.message)
    process.exitCode = 1
  }
}

async function main() {
  await test('401 without auth', async () => {
    const r = await fetch(`${BASE}/api/users/search?name=a`)
    if (r.status !== 401) throw new Error(`Expected 401, got ${r.status}`)
  })

  await test('200 with auth and response shape', async () => {
    const r = await fetch(`${BASE}/api/users/search?name=alice`, {
      headers: { Authorization: 'Bearer test-token' },
    })
    if (r.status !== 200) throw new Error(`Expected 200, got ${r.status}`)
    const data = await r.json()
    if (!Array.isArray(data.users) || typeof data.total !== 'number' || typeof data.hasMore !== 'boolean') {
      throw new Error('Invalid response shape')
    }
  })
}

main()
