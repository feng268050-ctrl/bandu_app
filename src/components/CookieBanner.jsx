import { useState } from 'react'

function ToggleRow({ label, description, checked, disabled = false, onToggle }) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3">
      <div>
        <p className="text-sm font-semibold text-slate-900">{label}</p>
        <p className="mt-1 text-xs text-slate-500">{description}</p>
      </div>

      <button
        type="button"
        disabled={disabled}
        onClick={onToggle}
        className={`relative h-6 w-11 rounded-full transition ${
          checked ? 'bg-emerald-500' : 'bg-slate-300'
        } ${disabled ? 'cursor-not-allowed opacity-60' : ''}`}
      >
        <span
          className={`absolute top-0.5 h-5 w-5 rounded-full bg-white transition ${
            checked ? 'left-[22px]' : 'left-0.5'
          }`}
        />
      </button>
    </div>
  )
}

export default function CookieBanner({ onAcceptAll, onRejectAll, onSaveSettings, locale = 'zh' }) {
  const zh = locale === 'zh'
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [cookieSettings, setCookieSettings] = useState({
    necessary: true,
    analytics: true,
    marketing: false,
  })

  const updateSetting = (key) => {
    if (key === 'necessary') return
    setCookieSettings((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleAcceptAll = () => {
    const accepted = { necessary: true, analytics: true, marketing: true }
    setCookieSettings(accepted)
    onAcceptAll?.(accepted)
  }

  const handleRejectAll = () => {
    const rejected = { necessary: true, analytics: false, marketing: false }
    setCookieSettings(rejected)
    onRejectAll?.(rejected)
  }

  const handleSaveSettings = () => {
    onSaveSettings?.(cookieSettings)
    setSettingsOpen(false)
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto max-w-[1800px] px-6 py-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="min-w-0">
            <h2 className="text-3xl font-semibold text-slate-900">{zh ? 'Cookie 设置' : 'Cookie Settings'}</h2>
            <p className="mt-2 max-w-5xl text-sm leading-6 text-slate-700">
              {zh
                ? '本网站使用 Cookie，包括第三方 Cookie 及相关技术（“Cookie”），以改善网站功能和运营，提升用户体验，并用于分析和广告目的。有关更多信息，请参阅我们的'
                : 'This website uses cookies, including third-party cookies, to improve functionality and user experience, and for analytics and advertising. For more information, please read our'}
              <a className="mx-1 font-semibold text-emerald-600 hover:text-emerald-700" href="#">
                {zh ? '隐私政策' : 'Privacy Policy'}
              </a>
              {zh ? '和' : 'and'}
              <a className="mx-1 font-semibold text-emerald-600 hover:text-emerald-700" href="#">
                {zh ? 'Cookie 政策' : 'Cookie Policy'}
              </a>
              {zh ? '。' : '.'}
            </p>
          </div>

          <div className="flex shrink-0 flex-wrap gap-3">
            <button
              onClick={() => setSettingsOpen((prev) => !prev)}
              className="rounded-xl border border-emerald-600 px-5 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-50"
            >
              {zh ? 'Cookie 设置' : 'Cookie Settings'}
            </button>
            <button
              onClick={handleRejectAll}
              className="rounded-xl bg-emerald-600 px-5 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
            >
              {zh ? '全部拒绝' : 'Reject All'}
            </button>
            <button
              onClick={handleAcceptAll}
              className="rounded-xl bg-emerald-500 px-5 py-2 text-sm font-semibold text-white hover:bg-emerald-600"
            >
              {zh ? '全部接受' : 'Accept All'}
            </button>
          </div>
        </div>

        {settingsOpen ? (
          <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="grid gap-3 lg:grid-cols-3">
              <ToggleRow
                label={zh ? '必要 Cookie' : 'Required Cookies'}
                description={zh ? '站点运行必须，无法关闭' : 'Required for site operation, cannot be disabled'}
                checked={cookieSettings.necessary}
                disabled
              />
              <ToggleRow
                label={zh ? '分析 Cookie' : 'Analytics Cookies'}
                description={zh ? '用于统计访问与性能分析' : 'Used for traffic and performance analytics'}
                checked={cookieSettings.analytics}
                onToggle={() => updateSetting('analytics')}
              />
              <ToggleRow
                label={zh ? '营销 Cookie' : 'Marketing Cookies'}
                description={zh ? '用于个性化推荐与广告' : 'Used for personalization and advertising'}
                checked={cookieSettings.marketing}
                onToggle={() => updateSetting('marketing')}
              />
            </div>

            <div className="mt-3 flex justify-end">
              <button
                onClick={handleSaveSettings}
                className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
              >
                {zh ? '保存设置' : 'Save Settings'}
              </button>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  )
}
