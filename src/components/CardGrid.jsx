import { useEffect, useMemo, useRef, useState } from 'react'
import { modelCards } from '../data/mockData'
import { IconCubeBadge } from './icons.jsx'

// 非推荐分类统一使用固定 25 张卡片流。
const MAX_NON_RECOMMENDED_ITEMS = 25
// 每次滚动增量加载 5 张。
const NON_RECOMMENDED_BATCH = 5
// 首屏先渲染的卡片数量。
const NON_RECOMMENDED_INITIAL = 10

function formatCount(value) {
  if (typeof value !== 'number') return '0'
  if (value >= 1000) {
    const short = value >= 10000 ? (value / 1000).toFixed(0) : (value / 1000).toFixed(1)
    return `${short}k`
  }
  return `${value}`
}

function RecommendedCard({ card, onOpen }) {
  const clickable = typeof onOpen === 'function'

  return (
    <article
      className={`group relative overflow-hidden rounded-[24px] bg-white ring-1 ring-slate-200 transition ${
        clickable ? 'cursor-pointer hover:-translate-y-0.5 hover:shadow-lg' : ''
      }`}
      onClick={clickable ? () => onOpen(card) : undefined}
    >
      <div className="absolute left-3 top-3 z-10 flex h-9 w-9 items-center justify-center rounded-xl bg-slate-900/70 text-lime-300 backdrop-blur-sm">
        <IconCubeBadge className="h-4 w-4" />
      </div>

      {card.category ? (
        <div className="absolute right-3 top-3 z-10 rounded-full bg-black/50 px-3 py-1 text-xs font-medium text-white backdrop-blur-sm">
          {card.category}
        </div>
      ) : null}

      <img
        src={card.image}
        alt={card.title}
        className="h-56 w-full object-cover transition duration-300 group-hover:scale-105"
        loading="lazy"
      />

      <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent p-4 text-white">
        <h4 className="text-lg font-semibold leading-6">{card.title}</h4>
        <p className="mt-1 text-xs text-white/80">{card.creator}</p>
        {card.summary ? <p className="mt-2 text-xs text-white/75">{card.summary}</p> : null}
      </div>
    </article>
  )
}

function GalleryCard({ card, onOpen, locale = 'zh' }) {
  const clickable = typeof onOpen === 'function'
  const zh = locale === 'zh'

  return (
    <article
      className={`group rounded-2xl transition ${clickable ? 'cursor-pointer hover:-translate-y-0.5' : ''}`}
      onClick={clickable ? () => onOpen(card) : undefined}
    >
      <div className="relative overflow-hidden rounded-2xl bg-white">
        <div className="absolute left-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-xl bg-slate-900/70 text-lime-300 backdrop-blur-sm">
          <IconCubeBadge className="h-4 w-4" />
        </div>
        <img
          src={card.image}
          alt={card.title}
          className="h-40 w-full object-cover transition duration-300 group-hover:scale-[1.02]"
          loading="lazy"
        />
      </div>

      <h4 className="mt-3 min-h-[48px] text-[16px] font-semibold leading-6 text-slate-900">{card.title}</h4>

      <div className="mt-3 flex items-center justify-between gap-2 text-sm text-slate-500">
        <div className="min-w-0 truncate">{card.creator}</div>
        <div className="flex shrink-0 items-center gap-3">
          <span>{formatCount(card.views)} {zh ? '浏览' : 'views'}</span>
          <span>{formatCount(card.likes)} {zh ? '赞' : 'likes'}</span>
        </div>
      </div>
    </article>
  )
}

function LoadingState() {
  return (
    <div className="mt-8 flex h-20 items-center justify-center rounded-2xl bg-white">
      <div className="h-10 w-10 animate-spin rounded-full border-[3px] border-emerald-400 border-r-transparent" />
    </div>
  )
}

export default function CardGrid({
  items = modelCards,
  onOpen,
  title = '推荐模型',
  subtitle = '点击卡片可查看详情页',
  locale = 'zh',
  isRecommendedBoard = true,
  showHeader = true,
}) {
  // 红框分类视为“非推荐模式”。
  const nonRecommendedMode = !isRecommendedBoard
  // 渐进加载所使用的观察哨兵节点。
  const sentinelRef = useRef(null)

  // 复制数据，确保非推荐模式固定展示 25 条。
  const preparedItems = useMemo(() => {
    if (!nonRecommendedMode || items.length === 0) return items

    return Array.from({ length: MAX_NON_RECOMMENDED_ITEMS }, (_, index) => {
      const source = items[index % items.length]
      return {
        ...source,
        id: `${source.id}-feed-${index}`,
      }
    })
  }, [items, nonRecommendedMode])

  // 当模式或数据量变化时，重置可见数量。
  const [visibleCount, setVisibleCount] = useState(
    nonRecommendedMode
      ? Math.min(NON_RECOMMENDED_INITIAL, preparedItems.length)
      : preparedItems.length
  )

  useEffect(() => {
    setVisibleCount(
      nonRecommendedMode
        ? Math.min(NON_RECOMMENDED_INITIAL, preparedItems.length)
        : preparedItems.length
    )
  }, [nonRecommendedMode, preparedItems.length])

  // 基于滚动的渐进加载（最多到 25 张）。
  useEffect(() => {
    if (!nonRecommendedMode || visibleCount >= MAX_NON_RECOMMENDED_ITEMS) return undefined
    const target = sentinelRef.current
    if (!target) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        const inView = entries.some((entry) => entry.isIntersecting)
        if (!inView) return
        setVisibleCount((prev) => Math.min(prev + NON_RECOMMENDED_BATCH, MAX_NON_RECOMMENDED_ITEMS))
      },
      { root: null, rootMargin: '220px 0px' }
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [nonRecommendedMode, visibleCount])

  const visibleItems = nonRecommendedMode
    ? preparedItems.slice(0, Math.min(visibleCount, MAX_NON_RECOMMENDED_ITEMS))
    : preparedItems

  const showLoader = nonRecommendedMode && visibleCount >= MAX_NON_RECOMMENDED_ITEMS && visibleItems.length > 0
  const totalCount = nonRecommendedMode ? MAX_NON_RECOMMENDED_ITEMS : items.length

  return (
    <section className="px-6 pb-8 pt-6">
      {showHeader ? (
        <div className="mb-5 flex items-center justify-between gap-3">
          <div>
            <h3 className="text-2xl font-bold text-slate-900">{title}</h3>
            <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
          </div>
          <div className="rounded-full bg-white px-4 py-2 text-sm text-slate-500 shadow-sm ring-1 ring-slate-200">
            共 {totalCount} 项
          </div>
        </div>
      ) : null}

      <div
        className={
          nonRecommendedMode
            ? 'grid grid-cols-1 gap-x-5 gap-y-7 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5'
            : 'grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4'
        }
      >
        {visibleItems.map((card) => (
          nonRecommendedMode ? (
            <GalleryCard key={card.id} card={card} onOpen={onOpen} locale={locale} />
          ) : (
            <RecommendedCard key={card.id} card={card} onOpen={onOpen} />
          )
        ))}
      </div>

      {nonRecommendedMode && visibleCount < MAX_NON_RECOMMENDED_ITEMS ? (
        <div ref={sentinelRef} className="h-6" />
      ) : null}

      {showLoader ? <LoadingState /> : null}
    </section>
  )
}
