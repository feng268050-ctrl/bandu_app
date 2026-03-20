import { IconGlobe, IconMenu, IconSearch } from './icons.jsx'

export default function Header({
  query = '',
  setQuery,
  onOpenMenu,
  locale = 'zh',
  onToggleLocale,
  onOpenLogin,
  currentUser = '',
  onLogout,
  compact = false,
  sticky = true,
}) {
  const controlled = typeof setQuery === 'function'
  const zh = locale === 'zh'
  const stickyClass = sticky ? 'sticky top-0 z-40 bg-[#f3f4f6]/95 backdrop-blur-sm' : ''
  const containerClass = compact
    ? `${stickyClass} flex items-center gap-3`
    : `${stickyClass} flex items-center gap-4 px-6 pb-4 pt-6`
  const searchClass = compact
    ? 'flex w-[460px] items-center gap-3 rounded-full border border-slate-200 bg-white px-4 py-2.5 shadow-[0_1px_0_rgba(15,23,42,0.04)] 2xl:w-[540px]'
    : 'flex flex-1 items-center gap-3 rounded-full border border-slate-200 bg-white px-5 py-3 shadow-[0_1px_0_rgba(15,23,42,0.04)]'

  return (
    <header className={containerClass}>
      {!compact ? (
        <button
          className="rounded-xl p-2 text-slate-600 hover:bg-white xl:hidden"
          aria-label={zh ? '打开菜单' : 'Open menu'}
          onClick={onOpenMenu}
        >
          <IconMenu className="h-5 w-5" />
        </button>
      ) : null}

      <label className={searchClass}>
        <IconSearch className="h-5 w-5 text-slate-400" />
        <input
          className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
          placeholder={zh ? '搜索模型、用户、收藏夹和动态' : 'Search models, users, collections and feeds'}
          value={controlled ? query : undefined}
          onChange={controlled ? (event) => setQuery(event.target.value) : undefined}
        />
      </label>

      <button
        className="inline-flex items-center gap-1 rounded-full p-2 text-slate-600 transition hover:bg-white"
        aria-label={zh ? '切换语言' : 'Switch language'}
        onClick={onToggleLocale}
        title={zh ? '切换到 English' : 'Switch to 中文'}
      >
        <IconGlobe className="h-5 w-5" />
        <span className="text-xs font-semibold">{zh ? '中' : 'EN'}</span>
      </button>

      {currentUser ? (
        <button
          className="rounded-full border border-slate-200 bg-white px-5 py-2 text-sm font-semibold text-slate-900 transition hover:bg-slate-50"
          onClick={onLogout}
          title={zh ? '点击退出登录' : 'Click to sign out'}
        >
          {zh ? `${currentUser} 退出` : `${currentUser} Sign out`}
        </button>
      ) : (
        <button
          className="rounded-full border border-slate-200 bg-white px-6 py-2 text-sm font-semibold text-slate-900 transition hover:bg-slate-50"
          onClick={onOpenLogin}
        >
          {zh ? '登录' : 'Login'}
        </button>
      )}
    </header>
  )
}
