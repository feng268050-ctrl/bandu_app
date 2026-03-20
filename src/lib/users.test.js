/**
 * Unit tests for user search logic.
 * Run: node --test src/lib/users.test.js
 */
import { describe, it } from 'node:test'
import assert from 'node:assert'
import { searchUsers } from './users.js'

describe('searchUsers', () => {
  it('search by name only (partial, case-insensitive)', () => {
    const { users, total, hasMore } = searchUsers({ name: 'alice' })
    assert.strictEqual(users.length, 1)
    assert.strictEqual(users[0].name, 'Alice Chen')
    assert.strictEqual(users[0].email, 'alice@example.com')
    assert.strictEqual(total, 1)
    assert.strictEqual(hasMore, false)
  })

  it('search by email only (partial, case-insensitive)', () => {
    const { users, total } = searchUsers({ email: '@example.com' })
    assert.ok(users.length >= 1)
    assert.ok(users.every((u) => u.email.toLowerCase().includes('@example.com')))
    assert.strictEqual(total, users.length)
  })

  it('search by name and email (AND)', () => {
    const { users, total } = searchUsers({ name: 'Alice', email: 'alice@example.com' })
    assert.strictEqual(users.length, 1)
    assert.strictEqual(users[0].name, 'Alice Chen')
    assert.strictEqual(total, 1)
  })

  it('no matches returns empty list', () => {
    const { users, total, hasMore } = searchUsers({ name: 'NonexistentUser123' })
    assert.strictEqual(users.length, 0)
    assert.strictEqual(total, 0)
    assert.strictEqual(hasMore, false)
  })

  it('empty or missing params returns empty', () => {
    const r1 = searchUsers({})
    assert.strictEqual(r1.users.length, 0)
    assert.strictEqual(r1.total, 0)
    const r2 = searchUsers({ name: '', email: '' })
    assert.strictEqual(r2.users.length, 0)
  })

  it('email fragment shorter than min returns empty', () => {
    const { users } = searchUsers({ email: 'ab' })
    assert.strictEqual(users.length, 0)
  })

  it('pagination: limit and offset', () => {
    const page1 = searchUsers({ name: 'a', limit: 2, offset: 0 })
    assert.strictEqual(page1.users.length, 2)
    assert.strictEqual(page1.hasMore, page1.total > 2)
    const page2 = searchUsers({ name: 'a', limit: 2, offset: 2 })
    assert.ok(page2.users.length <= 2)
  })

  it('result cap (max 50)', () => {
    const { users } = searchUsers({ name: 'a', limit: 100 })
    assert.ok(users.length <= 50)
  })

  it('each user has id, name, email', () => {
    const { users } = searchUsers({ name: 'alice' })
    assert.strictEqual(users.length, 1)
    assert.ok('id' in users[0])
    assert.ok('name' in users[0])
    assert.ok('email' in users[0])
  })
})
