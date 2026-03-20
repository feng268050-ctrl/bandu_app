import { useState } from 'react'

export default function LoginModal({ open, locale = 'zh', onClose, onSubmit }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const zh = locale === 'zh'

  if (!open) return null

  const canSubmit = username.trim() && password.trim()

  const handleSubmit = (event) => {
    event.preventDefault()
    if (!canSubmit) return
    onSubmit?.(username.trim())
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <div className="absolute inset-0 bg-black/30" onClick={onClose} />

      <form
        onSubmit={handleSubmit}
        className="relative z-10 w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">
            {zh ? '登录账号' : 'Sign in'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg px-2 py-1 text-slate-500 hover:bg-slate-100"
            aria-label={zh ? '关闭' : 'Close'}
          >
            X
          </button>
        </div>

        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-sm text-slate-600">{zh ? '用户名' : 'Username'}</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              placeholder={zh ? '请输入用户名' : 'Enter username'}
              className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-slate-400"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm text-slate-600">{zh ? '密码' : 'Password'}</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={zh ? '请输入密码' : 'Enter password'}
              className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-slate-400"
            />
          </label>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            {zh ? '取消' : 'Cancel'}
          </button>
          <button
            type="submit"
            disabled={!canSubmit}
            className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-400"
          >
            {zh ? '登录' : 'Login'}
          </button>
        </div>
      </form>
    </div>
  )
}
