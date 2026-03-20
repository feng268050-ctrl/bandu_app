'use client'

import { useEffect, useMemo, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import CardGrid from '../components/CardGrid.jsx'
import CategoryTabs from '../components/CategoryTabs.jsx'
import CommunityBoard from '../components/CommunityBoard.jsx'
import ContestBoard from '../components/ContestBoard.jsx'
import CookieBanner from '../components/CookieBanner.jsx'
import Header from '../components/Header.jsx'
import HeroSection from '../components/HeroSection.jsx'
import LaserCutModelsBoard from '../components/LaserCutModelsBoard.jsx'
import LoginModal from '../components/LoginModal.jsx'
import ProductDetailView from '../components/ProductDetailView.jsx'
import Sidebar from '../components/Sidebar.jsx'
import { modelCards } from '../data/mockData'
import { buildSidebarCollapsedCookie } from '../lib/sidebarPreferences'

const NAV_LABELS = {
  home: { zh: '首页', en: 'Home' },
  'all-models': { zh: 'Laser & Cut Models', en: 'Laser & Cut Models' },
  makerlab: { zh: 'MakerLab', en: 'MakerLab' },
  contest: { zh: '竞赛', en: 'Contest' },
  parts: { zh: '创客宝库', en: 'Parts Hub' },
  community: { zh: '社区', en: 'Community' },
}

const CATEGORY_LABELS_EN = {
  推荐: 'Recommended',
  热门: 'Popular',
  家用: 'Home',
  玩具和游戏: 'Toys & Games',
  '爱好和 DIY': 'Hobby & DIY',
  '3D打印机': 'CNC',
  艺术: 'Art',
  微缩模型: 'Miniatures',
  生成器模型: 'Generator Models',
  '激光&刀切': 'Laser & Cutting',
}

const INTERNAL_NAV_PATH_MAP = {
  home: '',
  'all-models': 'laser-cut-models',
  contest: 'contests',
  community: 'community',
}

const SEGMENT_ROUTE_MAP = {
  '': 'home',
  'laser-cut-models': 'all-models',
  contests: 'contest',
  community: 'community',
}

const EXTERNAL_NAV_URL_MAP = {
  makerlab: 'https://makerlab.bambulab.com',
  parts: 'https://makerworld.com/parts',
}

function resolveNavStateByRoute(routeId = 'home') {
  if (routeId === 'community') {
    return { mainNav: 'all-models', exploreNav: 'community' }
  }
  if (routeId === 'contest') {
    return { mainNav: 'contest', exploreNav: '' }
  }
  if (routeId === 'all-models') {
    return { mainNav: 'all-models', exploreNav: '' }
  }
  return { mainNav: 'home', exploreNav: '' }
}

function resolveRouteByPathname(pathname = '') {
  const segments = pathname.split('/').filter(Boolean)
  if (segments.length > 2) return null

  const [localeSegment, routeSegment = ''] = segments
  if (localeSegment !== 'zh' && localeSegment !== 'en') return null
  if (!(routeSegment in SEGMENT_ROUTE_MAP)) return null

  return {
    locale: localeSegment,
    routeId: SEGMENT_ROUTE_MAP[routeSegment],
  }
}

function buildInternalPath(locale, routeId) {
  const localePrefix = locale === 'zh' ? '/zh' : '/en'
  const segment = INTERNAL_NAV_PATH_MAP[routeId] ?? ''
  return segment ? `${localePrefix}/${segment}` : localePrefix
}

export default function InteractiveMakerWorldPage({
  initialLocale = 'zh',
  initialRouteId = 'home',
  initialSidebarCollapsed = false,
}) {
  const router = useRouter()
  const pathname = usePathname()
  const initialNavState = resolveNavStateByRoute(initialRouteId)

  const [locale, setLocale] = useState(initialLocale)
  const [activeCategory, setActiveCategory] = useState('推荐')
  const [query, setQuery] = useState('')
  const [selectedItem, setSelectedItem] = useState(null)
  const [activeMainNav, setActiveMainNav] = useState(initialNavState.mainNav)
  const [activeExploreNav, setActiveExploreNav] = useState(initialNavState.exploreNav)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(initialSidebarCollapsed)
  const [cookieBannerVisible, setCookieBannerVisible] = useState(true)
  const [cookiePreferenceLabel, setCookiePreferenceLabel] = useState('')
  const [loginModalOpen, setLoginModalOpen] = useState(false)
  const [currentUser, setCurrentUser] = useState('')

  const zh = locale === 'zh'
  // 模型看板页面使用独立布局。
  const isAllModelsBoard = activeMainNav === 'all-models'
  const isContestBoard = activeMainNav === 'contest'
  const isCommunityBoard = activeExploreNav === 'community'
  // 首页下的推荐分类和其他分类采用不同卡片布局。
  const isRecommendedCategory = activeCategory === '推荐'
  // 仅首页启用“分类+搜索”整栏吸顶。
  const isHomeBoard = activeMainNav === 'home' && !activeExploreNav

  const selectedSidebarLabel = useMemo(() => {
    const currentId = activeExploreNav || activeMainNav
    return NAV_LABELS[currentId]?.[locale] ?? (zh ? '首页' : 'Home')
  }, [activeExploreNav, activeMainNav, locale, zh])

  const filteredModels = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return modelCards.filter((card) => {
      const matchCategory = activeCategory === '推荐' || card.category === activeCategory
      const matchMainNav =
        activeMainNav === 'home' ||
        activeMainNav === 'all-models' ||
        (activeMainNav === 'makerlab' && card.creator.toLowerCase().includes('makerlab')) ||
        (activeMainNav === 'contest' && card.category === '教育')

      const matchExploreNav =
        !activeExploreNav ||
        (activeExploreNav === 'parts' && ['工具', '家用'].includes(card.category)) ||
        (activeExploreNav === 'community' && card.likes >= 220)

      const searchableText = [
        card.title,
        card.creator,
        card.category,
        card.summary,
        card.description,
        ...(card.tags ?? []),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()

      const matchQuery = !normalizedQuery || searchableText.includes(normalizedQuery)
      return matchCategory && matchMainNav && matchExploreNav && matchQuery
    })
  }, [activeCategory, query, activeMainNav, activeExploreNav])

  useEffect(() => {
    const parsedRoute = resolveRouteByPathname(pathname)
    if (!parsedRoute) return

    const nextNavState = resolveNavStateByRoute(parsedRoute.routeId)
    setLocale((prev) => (prev === parsedRoute.locale ? prev : parsedRoute.locale))
    setActiveMainNav((prev) => (prev === nextNavState.mainNav ? prev : nextNavState.mainNav))
    setActiveExploreNav((prev) => (prev === nextNavState.exploreNav ? prev : nextNavState.exploreNav))
  }, [pathname])

  const syncPathByNav = (nextLocale, nextMainNav, nextExploreNav = '') => {
    const routeId = nextExploreNav && INTERNAL_NAV_PATH_MAP[nextExploreNav] !== undefined
      ? nextExploreNav
      : nextMainNav

    if (INTERNAL_NAV_PATH_MAP[routeId] === undefined) return

    const nextPath = buildInternalPath(nextLocale, routeId)
    if (pathname === nextPath) return
    router.push(nextPath)
  }

  const handleSelectMainNav = (id) => {
    setActiveMainNav(id)
    setActiveExploreNav('')

    if (id === 'home' || id === 'all-models') {
      setQuery('')
      setActiveCategory('推荐')
    }
    if (id === 'makerlab') {
      setQuery('makerlab')
      setActiveCategory('推荐')
    }
    if (id === 'contest') {
      setActiveCategory('推荐')
      setQuery('')
    }

    syncPathByNav(locale, id, '')
  }

  const handleSelectExploreNav = (id) => {
    setActiveExploreNav(id)
    setActiveMainNav('all-models')
    setActiveCategory('推荐')
    setQuery('')
    syncPathByNav(locale, 'all-models', id)
  }

  const handleOpenExternalNav = (id) => {
    if (typeof window === 'undefined') return
    const targetUrl = EXTERNAL_NAV_URL_MAP[id]
    if (!targetUrl) return
    window.location.href = targetUrl
  }

  const handleLoginSubmit = (username) => {
    setCurrentUser(username)
    setLoginModalOpen(false)
  }

  const handleToggleSidebarCollapse = () => {
    const nextSidebarCollapsed = !sidebarCollapsed
    setSidebarCollapsed(nextSidebarCollapsed)

    if (typeof document !== 'undefined') {
      document.cookie = buildSidebarCollapsedCookie(nextSidebarCollapsed)
    }
  }

  const handleToggleLocale = () => {
    const nextLocale = locale === 'zh' ? 'en' : 'zh'
    setLocale(nextLocale)
    syncPathByNav(nextLocale, activeMainNav, activeExploreNav)
  }

  if (selectedItem) {
    return <ProductDetailView item={selectedItem} onBack={() => setSelectedItem(null)} locale={locale} />
  }

  return (
    <div className="min-h-screen bg-[#f3f4f6] text-slate-900">
      <div className="mx-auto w-full">
        <Sidebar
          activeMainId={activeMainNav}
          activeExploreId={activeExploreNav}
          onSelectMain={handleSelectMainNav}
          onSelectExplore={handleSelectExploreNav}
          onOpenExternal={handleOpenExternalNav}
          collapsed={sidebarCollapsed}
          onToggleCollapse={handleToggleSidebarCollapse}
          locale={locale}
        />

        <main className={`min-w-0 pb-36 transition-[margin] duration-300 ease-out ${sidebarCollapsed ? 'xl:ml-[72px]' : 'xl:ml-[248px]'}`}>
          {isAllModelsBoard ? (
            // 模型看板页面仅保留顶部整行搜索区。
            <Header
              query={query}
              setQuery={setQuery}
              locale={locale}
              onToggleLocale={handleToggleLocale}
              onOpenLogin={() => setLoginModalOpen(true)}
              currentUser={currentUser}
              onLogout={() => setCurrentUser('')}
            />
          ) : isContestBoard ? null : (
            <>
              <div className="xl:hidden">
                <Header
                  query={query}
                  setQuery={setQuery}
                  locale={locale}
                  onToggleLocale={handleToggleLocale}
                  onOpenLogin={() => setLoginModalOpen(true)}
                  currentUser={currentUser}
                  onLogout={() => setCurrentUser('')}
                />
                <CategoryTabs
                  activeCategory={activeCategory}
                  setActiveCategory={setActiveCategory}
                  labelMap={zh ? undefined : CATEGORY_LABELS_EN}
                />
              </div>

              <div
                className={`hidden items-start gap-4 px-6 pb-4 pt-6 xl:flex ${
                  isHomeBoard ? 'sticky top-0 z-40 bg-[#f3f4f6]/95 backdrop-blur-sm' : ''
                }`}
              >
                <div className="min-w-0 flex-1">
                  <CategoryTabs
                    activeCategory={activeCategory}
                    setActiveCategory={setActiveCategory}
                    labelMap={zh ? undefined : CATEGORY_LABELS_EN}
                    inline
                  />
                </div>
                <Header
                  query={query}
                  setQuery={setQuery}
                  locale={locale}
                  onToggleLocale={handleToggleLocale}
                  onOpenLogin={() => setLoginModalOpen(true)}
                  currentUser={currentUser}
                  onLogout={() => setCurrentUser('')}
                  sticky={!isHomeBoard}
                  compact
                />
              </div>
            </>
          )}

          {isCommunityBoard ? (
            <CommunityBoard locale={locale} />
          ) : isContestBoard ? (
            <ContestBoard locale={locale} query={query} setQuery={setQuery} />
          ) : isAllModelsBoard ? (
            // 按照页面要求，此处不显示参考图中的黄色切换栏。
            <LaserCutModelsBoard
              items={filteredModels}
              onOpen={setSelectedItem}
              locale={locale}
            />
          ) : (
            <>
              {/* 非推荐分类隐藏顶部 Hero 区域。 */}
              {isRecommendedCategory ? <HeroSection locale={locale} /> : null}
              <CardGrid
                items={filteredModels}
                onOpen={setSelectedItem}
                title={zh ? `${selectedSidebarLabel} · 推荐模型` : `${selectedSidebarLabel} · Recommended`}
                subtitle={
                  zh
                    ? '支持左侧导航、分类和搜索，点击卡片可查看假详情页'
                    : 'Supports sidebar navigation, category filter and search. Click a card to open demo detail page.'
                }
                locale={locale}
                isRecommendedBoard={isRecommendedCategory}
                // 非推荐分类隐藏标题/副标题/数量区块。
                showHeader={isRecommendedCategory}
              />
              {filteredModels.length === 0 ? (
                <div className="px-6 pb-8">
                  <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-8 text-center text-slate-500">
                    {zh
                      ? '没有匹配结果，试试其他关键词或分类。'
                      : 'No matching results. Try another keyword or category.'}
                  </div>
                </div>
              ) : null}
            </>
          )}
        </main>
      </div>

      {cookieBannerVisible ? (
        <CookieBanner
          locale={locale}
          onAcceptAll={() => {
            setCookiePreferenceLabel(zh ? '已接受全部 Cookie' : 'Accepted all cookies')
            setCookieBannerVisible(false)
          }}
          onRejectAll={() => {
            setCookiePreferenceLabel(zh ? '已拒绝全部 Cookie' : 'Rejected all cookies')
            setCookieBannerVisible(false)
          }}
          onSaveSettings={(settings) => {
            const savedLabel = settings.analytics || settings.marketing
              ? zh
                ? '已保存 Cookie 设置（自定义）'
                : 'Cookie settings saved (custom)'
              : zh
                ? '已保存 Cookie 设置（最小化）'
                : 'Cookie settings saved (minimal)'
            setCookiePreferenceLabel(savedLabel)
            setCookieBannerVisible(false)
          }}
        />
      ) : (
        <button
          onClick={() => setCookieBannerVisible(true)}
          className="fixed bottom-4 right-4 z-40 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50"
        >
          {cookiePreferenceLabel || (zh ? 'Cookie 设置' : 'Cookie Settings')}
        </button>
      )}

      <LoginModal
        open={loginModalOpen}
        locale={locale}
        onClose={() => setLoginModalOpen(false)}
        onSubmit={handleLoginSubmit}
      />
    </div>
  )
}
