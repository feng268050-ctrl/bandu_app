'use client'

import { useState } from 'react'

/**
 * Minimal admin UI for user search.
 * In production, protect this route (e.g. middleware or auth) so only authorized users can reach it.
 * The API requires an Authorization token; enter it below to search.
 */
export default function AdminUserSearchPage() {
  const [token, setToken] = useState('')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [results, setResults] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!token.trim()) {
      setError('Please enter an API token.')
      return
    }
    const params = new URLSearchParams()
    if (name.trim()) params.set('name', name.trim())
    if (email.trim()) params.set('email', email.trim())
    if (!params.toString()) {
      setError('Enter at least name or email to search.')
      return
    }
    setError(null)
    setLoading(true)
    setResults(null)
    try {
      const res = await fetch(`/api/users/search?${params}`, {
        headers: { Authorization: `Bearer ${token.trim()}` },
      })
      const data = await res.json()
      if (!res.ok) {
        setError(data.error || `Error ${res.status}`)
        setResults(null)
        return
      }
      setResults(data)
    } catch (err) {
      setError(err.message || 'Request failed')
      setResults(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="mx-auto max-w-2xl">
        <h1 className="mb-2 text-2xl font-semibold text-gray-900">User Search (Admin)</h1>
        <p className="mb-6 text-sm text-gray-600">
          This page is for authorized users only. Protect this route in production (e.g. middleware or auth).
        </p>
        <form onSubmit={handleSubmit} className="mb-8 space-y-4 rounded-lg bg-white p-6 shadow">
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">API token *</label>
            <input
              type="password"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Bearer token or API key"
              className="w-full rounded border border-gray-300 px-3 py-2 text-gray-900"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Name (optional)</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Partial match"
              className="w-full rounded border border-gray-300 px-3 py-2 text-gray-900"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">Email (optional, min 3 chars)</label>
            <input
              type="text"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Partial match"
              className="w-full rounded border border-gray-300 px-3 py-2 text-gray-900"
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? 'Searching…' : 'Search'}
          </button>
        </form>
        {results && (
          <div className="rounded-lg bg-white p-6 shadow">
            <h2 className="mb-4 text-lg font-medium text-gray-900">
              Results ({results.total}) {results.hasMore && '(more available)'}
            </h2>
            <ul className="space-y-2">
              {results.users.length === 0 ? (
                <li className="text-gray-500">No users found.</li>
              ) : (
                results.users.map((u) => (
                  <li key={u.id} className="rounded border border-gray-200 p-3">
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                      <span className="font-medium text-gray-900">{u.name}</span>
                      <span className="text-gray-600">{u.email}</span>
                      <span className="text-gray-400 text-sm">id: {u.id}</span>
                    </div>
                  </li>
                ))
              )}
            </ul>
          </div>
        )}
      </div>
    </div>
  )
}
