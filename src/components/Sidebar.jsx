import { useLayoutEffect, useRef, useState } from 'react'
import { sidebarExplore, sidebarMain } from '../data/mockData'
import {
  IconBoxes,
  IconExternalLink,
  IconFlask,
  IconFacebook,
  IconHome,
  IconInstagram,
  IconMenuPanel,
  IconPackage,
  IconTiktok,
  IconTrophy,
  IconUsers,
  IconXBrand,
  IconYoutube,
} from './icons.jsx'

const mainIconMap = {
  home: IconHome,
  'all-models': IconBoxes,
  makerlab: IconFlask,
  contest: IconTrophy,
}

const exploreIconMap = {
  parts: IconPackage,
  community: IconUsers,
}

const mainLabelMap = {
  home: { zh: '首页', en: 'Home' },
  'all-models': { zh: 'Laser & Cut Models', en: 'Laser & Cut Models' },
  makerlab: { zh: 'MakerLab', en: 'MakerLab' },
  contest: { zh: '竞赛', en: 'Contest' },
}

const exploreLabelMap = {
  parts: { zh: '创客宝库', en: 'Parts Hub' },
  community: { zh: '社区', en: 'Community' },
}

const externalNavIds = new Set(['makerlab', 'parts'])

const footerSocialMap = {
  facebook: IconFacebook,
  instagram: IconInstagram,
  youtube: IconYoutube,
  x: IconXBrand,
  tiktok: IconTiktok,
}

const footerContent = {
  zh: {
    socials: ['facebook', 'instagram', 'youtube', 'x', 'tiktok'],
    firstLine: ['隐私', '用户协议', '社区准则', '常见问题'],
    secondLine: ['Cookie 设置', '热门搜索'],
  },
  en: {
    socials: ['facebook', 'instagram', 'youtube', 'x', 'tiktok'],
    firstLine: ['Privacy', 'User Terms', 'Community Rules', 'FAQ'],
    secondLine: ['Cookie Settings', 'Trending'],
  },
}

function LogoMark() {
  return (
    <div className="grid h-[22px] w-[22px] grid-cols-2 gap-[3px] rounded-[3px] bg-gradient-to-br from-slate-100 to-slate-200 p-[3px]">
      <span className="rounded-[4px] border border-slate-500 bg-white" />
      <span className="rounded-[4px] border border-slate-500 bg-white" />
      <span className="rounded-[4px] border border-slate-500 bg-white" />
      <span className="rounded-[4px] border border-slate-500 bg-white" />
    </div>
  )
}

function SidebarItem({
  label,
  icon,
  active = false,
  onClick,
  collapsed = false,
  external = false,
  testId,
}) {
  const containerClass = `flex w-full items-center rounded-2xl text-left transition ${
    collapsed
      ? 'justify-center px-2 py-3'
      : external
        ? 'justify-between gap-2 px-3 py-2.5'
        : 'gap-3 px-4 py-3'
  } ${
    external
      ? 'font-medium text-slate-800 hover:bg-slate-100/70'
      : active
        ? 'bg-slate-100 font-semibold text-slate-900'
        : 'font-medium text-slate-700 hover:bg-slate-100/70'
  }`

  return (
    <button
      onClick={onClick}
      aria-pressed={active}
      title={collapsed ? label : undefined}
      className={containerClass}
      data-testid={testId}
      type="button"
    >
      {collapsed ? (
        <span className={external || active ? 'text-slate-900' : 'text-slate-600'}>{icon}</span>
      ) : external ? (
        <>
          <span className="flex min-w-0 items-center gap-3">
            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-slate-300 text-slate-700 [&_svg]:h-[12px] [&_svg]:w-[12px]">
              {icon}
            </span>
            <span className="truncate text-[13px]">{label}</span>
          </span>
          <IconExternalLink className="h-[15px] w-[15px] shrink-0 text-slate-600" />
        </>
      ) : (
        <>
          <span className={active ? 'text-slate-900' : 'text-slate-600'}>{icon}</span>
          <span className="text-[13px]">{label}</span>
        </>
      )}
    </button>
  )
}

export default function Sidebar({
  activeMainId = 'home',
  activeExploreId = '',
  onSelectMain,
  onSelectExplore,
  onOpenExternal,
  collapsed = false,
  onToggleCollapse,
  locale = 'zh',
}) {
  const canSelectMain = typeof onSelectMain === 'function'
  const canSelectExplore = typeof onSelectExplore === 'function'
  const canOpenExternal = typeof onOpenExternal === 'function'
  const zh = locale === 'zh'
  const footerText = footerContent[locale] ?? footerContent.zh
  const isCollapsedView = collapsed
  const [sidebarAnimating, setSidebarAnimating] = useState(false)
  const showExpandedContent = !isCollapsedView && !sidebarAnimating
  const asideRef = useRef(null)
  const prevCollapsedRef = useRef(collapsed)

  useLayoutEffect(() => {
    if (prevCollapsedRef.current !== collapsed) {
      prevCollapsedRef.current = collapsed
      setSidebarAnimating(true)
      const aside = asideRef.current
      const onEnd = (e) => {
        if (e.propertyName === 'width') {
          setSidebarAnimating(false)
          aside?.removeEventListener('transitionend', onEnd)
        }
      }
      aside?.addEventListener('transitionend', onEnd)
      const t = setTimeout(() => {
        setSidebarAnimating(false)
        aside?.removeEventListener('transitionend', onEnd)
      }, 320)
      return () => {
        clearTimeout(t)
        aside?.removeEventListener('transitionend', onEnd)
      }
    }
  }, [collapsed])

  return (
    <aside
      ref={asideRef}
      data-testid="app-sidebar"
      className={`hidden shrink-0 border-r border-slate-200 bg-white transition-[width] duration-300 ease-out xl:fixed xl:inset-y-0 xl:left-0 xl:z-30 xl:flex xl:flex-col ${sidebarAnimating ? 'will-change-[width]' : ''} ${
        isCollapsedView ? 'w-[72px]' : 'w-[248px]'
      }`}
    >
      <div
        className={`flex pb-5 pt-8 ${
          isCollapsedView ? 'flex-col items-center gap-3 px-2' : 'items-center justify-between px-6'
        }`}
      >
        <div className={`flex items-center ${isCollapsedView ? '' : 'gap-3'}`}>
          <LogoMark />
          <div
            aria-hidden={!showExpandedContent}
            tabIndex={!showExpandedContent ? -1 : undefined}
            className={`overflow-hidden transition-opacity duration-200 ease-out ${
              showExpandedContent
                ? 'opacity-100'
                : 'min-w-0 w-0 opacity-0 pointer-events-none invisible'
            }`}
          >
            <div className="leading-tight text-slate-900 whitespace-nowrap">
              <p className="text-[12px] font-semibold">Laser</p>
              <p className="text-[12px] font-semibold">Cyber</p>
            </div>
          </div>
        </div>

        <button
          className="rounded-xl p-2 text-slate-600 hover:bg-slate-100 active:scale-95 transition-transform duration-300 ease-out"
          onClick={onToggleCollapse}
          data-testid="sidebar-toggle"
          title={collapsed ? (zh ? '展开侧边栏' : 'Expand sidebar') : zh ? '收起侧边栏' : 'Collapse sidebar'}
        >
          <IconMenuPanel className={`h-5 w-5 transition-transform duration-300 ease-out ${isCollapsedView ? 'rotate-180' : 'rotate-0'}`} />
        </button>
      </div>

      <div className={`flex-1 overflow-y-auto pb-8 ${isCollapsedView ? 'px-2' : 'px-4'}`}>
        <nav className="space-y-1">
          {sidebarMain.map((item, index) => {
            const ItemIcon = mainIconMap[item.id] ?? IconBoxes
            const label = mainLabelMap[item.id]?.[locale] ?? item.label
            const external = externalNavIds.has(item.id)

            return (
              <SidebarItem
                key={item.id}
                label={label}
                icon={<ItemIcon className="h-[18px] w-[18px]" />}
                active={external ? false : canSelectMain ? activeMainId === item.id : index === 0}
                onClick={
                  external
                    ? canOpenExternal
                      ? () => onOpenExternal(item.id)
                      : undefined
                    : canSelectMain
                      ? () => onSelectMain(item.id)
                      : undefined
                }
                collapsed={isCollapsedView}
                external={external}
                testId={`sidebar-item-${item.id}`}
              />
            )
          })}
        </nav>

        <div
          aria-hidden={!showExpandedContent}
          tabIndex={!showExpandedContent ? -1 : undefined}
          className={`mb-2 mt-8 px-2 text-[18px] font-semibold text-slate-900 transition-opacity duration-200 ease-out ${
            showExpandedContent ? 'opacity-100' : 'opacity-0 pointer-events-none invisible h-0 overflow-hidden mt-0 mb-0'
          }`}
        >
          {zh ? '探索' : 'Explore'}
        </div>
        <div
          aria-hidden={!isCollapsedView}
          className={`mx-auto my-6 h-px w-10 bg-slate-200 transition-opacity duration-200 ease-out ${
            isCollapsedView ? 'opacity-100' : 'opacity-0 pointer-events-none h-0 overflow-hidden my-0'
          }`}
        />

        <nav className="space-y-1">
          {sidebarExplore.map((item) => {
            const ItemIcon = exploreIconMap[item.id] ?? IconBoxes
            const label = exploreLabelMap[item.id]?.[locale] ?? item.label
            const external = externalNavIds.has(item.id)

            return (
              <SidebarItem
                key={item.id}
                label={label}
                icon={<ItemIcon className="h-[18px] w-[18px]" />}
                active={external ? false : canSelectExplore ? activeExploreId === item.id : false}
                onClick={
                  external
                    ? canOpenExternal
                      ? () => onOpenExternal(item.id)
                      : undefined
                    : canSelectExplore
                      ? () => onSelectExplore(item.id)
                      : undefined
                }
                collapsed={isCollapsedView}
                external={external}
                testId={`sidebar-item-${item.id}`}
              />
            )
          })}
        </nav>
      </div>

      <footer
        aria-hidden={!showExpandedContent}
        tabIndex={!showExpandedContent ? -1 : undefined}
        className={`mt-auto border-t border-slate-200 px-6 pb-6 pt-6 text-slate-600 transition-[opacity,max-height] duration-300 ease-out ${
          showExpandedContent
            ? 'opacity-100 max-h-[240px] overflow-hidden'
            : 'opacity-0 pointer-events-none invisible max-h-0 overflow-hidden'
        }`}
      >
        <div className="flex items-center gap-4">
          {footerText.socials.map((item) => {
            const Icon = footerSocialMap[item]
            return (
              <button
                key={item}
                tabIndex={!showExpandedContent ? -1 : undefined}
                className="rounded-md bg-slate-500 p-1.5 text-white transition hover:bg-slate-600"
                aria-label={item}
                type="button"
              >
                <Icon className="h-3.5 w-3.5" />
              </button>
            )
          })}
        </div>

        <div className="mt-4 border-t border-slate-200 pt-3 text-[12px] font-medium leading-6">
          <p>{footerText.firstLine.join(' ')}</p>
          <p>{footerText.secondLine.join(' ')}</p>
        </div>

        <p className="mt-4 text-[12px] font-medium leading-tight text-slate-500">
          &copy; 2026 LaserCyber
        </p>
      </footer>
    </aside>
  )
}
