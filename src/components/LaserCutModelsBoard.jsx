import { useEffect, useMemo, useRef, useState } from 'react'
import { modelCards } from '../data/mockData'
import { IconCubeBadge } from './icons.jsx'

// 与参考布局保持一致：固定展示 24 张卡片。
const LASER_BOARD_TOTAL = 24
// 右侧卡片区的渐进加载参数。
const LASER_BOARD_INITIAL = 10
const LASER_BOARD_BATCH = 5

const SORT_OPTIONS = [
  { id: 'hot', zh: '热门', en: 'Hot' },
  { id: 'boost', zh: '助力数', en: 'Boosts' },
  { id: 'latest', zh: '最新', en: 'Latest' },
  { id: 'downloads', zh: '下载量', en: 'Downloads' },
  { id: 'likes', zh: '点赞量', en: 'Likes' },
  { id: 'random', zh: '随机', en: 'Random' },
]

const EVENT_OPTIONS = [
  { id: 'all', zh: '所有事件', en: 'All Events', sortId: 'hot' },
  { id: 'popular', zh: '热门事件', en: 'Popular Events', sortId: 'hot' },
  { id: 'download', zh: '下载事件', en: 'Download Events', sortId: 'downloads' },
  { id: 'like', zh: '点赞事件', en: 'Like Events', sortId: 'likes' },
  { id: 'latest', zh: '最新事件', en: 'Latest Events', sortId: 'latest' },
]

const TIME_OPTIONS = [
  { id: '24h', zh: '24 小时', en: '24 Hours' },
  { id: '7d', zh: '7天内', en: 'Last 7 Days' },
  { id: '30d', zh: '30天内', en: 'Last 30 Days' },
  { id: '1y', zh: '1年内', en: 'Last 1 Year' },
  { id: 'all', zh: '所有时间', en: 'All Time' },
]

const MODULE_FILTER_GROUPS = [
  { id: '激光模组', options: ['10W', '40W'] },
  { id: '刀切模组', options: ['精细尖刀', '笔'] },
]

const FILTER_SECTIONS = [
  { title: '模块', type: 'module-groups', groups: MODULE_FILTER_GROUPS },
  { title: '选择内容选项', options: ['是否有激光配置', '带有教程步骤'] },
  { title: '材料', options: ['铜', '铝', '铁', '碳钢'] },
  { title: '模型标签', options: ['点赞量', '下载量'] },
]

function formatCount(value) {
  if (typeof value !== 'number') return '0'
  if (value >= 1000) {
    const short = value >= 10000 ? (value / 1000).toFixed(0) : (value / 1000).toFixed(1)
    return `${short}k`
  }
  return `${value}`
}

function LaserCard({ item, onOpen, locale = 'zh' }) {
  const zh = locale === 'zh'
  const clickable = typeof onOpen === 'function'

  return (
    <article
      className={`group ${clickable ? 'cursor-pointer' : ''}`}
      onClick={clickable ? () => onOpen(item) : undefined}
    >
      <div className="relative overflow-hidden rounded-2xl bg-white">
        <div className="absolute left-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-xl bg-slate-900/70 text-lime-300 backdrop-blur-sm">
          <IconCubeBadge className="h-4 w-4" />
        </div>
        <img
          src={item.image}
          alt={item.title}
          className="h-64 w-full object-cover transition duration-300 group-hover:scale-[1.02]"
          loading="lazy"
        />
      </div>

      <h4 className="mt-3 min-h-[50px] text-[16px] font-semibold leading-6 text-slate-900">{item.title}</h4>

      <div className="mt-2 flex items-center justify-between gap-2 text-sm text-slate-500">
        <p className="min-w-0 truncate">{item.creator}</p>
        <div className="flex shrink-0 items-center gap-3">
          <span>{formatCount(item.views)} {zh ? '下载' : 'dl'}</span>
          <span>{formatCount(item.likes)} {zh ? '赞' : 'likes'}</span>
        </div>
      </div>
    </article>
  )
}

function LoadingState() {
  return (
    <div className="mt-8 flex items-center justify-center">
      <div className="h-10 w-10 animate-spin rounded-full border-[3px] border-emerald-400 border-r-transparent" />
    </div>
  )
}

export default function LaserCutModelsBoard({
  items = modelCards,
  locale = 'zh',
  onOpen,
}) {
  const zh = locale === 'zh'
  const [filtersVisible, setFiltersVisible] = useState(true)
  const [activeSort, setActiveSort] = useState('hot')
  const [selectedEvent, setSelectedEvent] = useState('all')
  const [eventDropdownOpen, setEventDropdownOpen] = useState(false)
  const [selectedTime, setSelectedTime] = useState('all')
  const [timeDropdownOpen, setTimeDropdownOpen] = useState(false)
  const [randomSeed, setRandomSeed] = useState(0)
  const [visibleCount, setVisibleCount] = useState(LASER_BOARD_INITIAL)
  const [selectedFilterOptions, setSelectedFilterOptions] = useState({})
  const eventDropdownRef = useRef(null)
  const timeDropdownRef = useRef(null)
  const loadMoreSentinelRef = useRef(null)
  const [openSections, setOpenSections] = useState(() =>
    Object.fromEntries(FILTER_SECTIONS.map((section) => [section.title, true]))
  )
  const [openModuleGroups, setOpenModuleGroups] = useState(() =>
    Object.fromEntries(MODULE_FILTER_GROUPS.map((group) => [group.id, true]))
  )

  // 工具栏时间按钮的当前文案。
  const selectedTimeLabel = useMemo(() => {
    const option = TIME_OPTIONS.find((item) => item.id === selectedTime)
    if (!option) return zh ? '所有时间' : 'All Time'
    return zh ? option.zh : option.en
  }, [selectedTime, zh])

  // 事件下拉按钮的当前文案。
  const selectedEventLabel = useMemo(() => {
    const option = EVENT_OPTIONS.find((item) => item.id === selectedEvent)
    if (!option) return zh ? '所有事件' : 'All Events'
    return zh ? option.zh : option.en
  }, [selectedEvent, zh])

  // 点击外部区域时关闭事件下拉框。
  useEffect(() => {
    if (!eventDropdownOpen) return undefined

    const handleClickOutside = (event) => {
      if (!eventDropdownRef.current?.contains(event.target)) {
        setEventDropdownOpen(false)
      }
    }

    window.addEventListener('mousedown', handleClickOutside)
    return () => window.removeEventListener('mousedown', handleClickOutside)
  }, [eventDropdownOpen])

  // 点击外部区域时关闭时间下拉框。
  useEffect(() => {
    if (!timeDropdownOpen) return undefined

    const handleClickOutside = (event) => {
      if (!timeDropdownRef.current?.contains(event.target)) {
        setTimeDropdownOpen(false)
      }
    }

    window.addEventListener('mousedown', handleClickOutside)
    return () => window.removeEventListener('mousedown', handleClickOutside)
  }, [timeDropdownOpen])

  const toggleSection = (title) => {
    setOpenSections((prev) => ({
      ...prev,
      [title]: !prev[title],
    }))
  }

  const toggleModuleGroup = (id) => {
    setOpenModuleGroups((prev) => ({
      ...prev,
      [id]: !prev[id],
    }))
  }

  const toggleFilterOption = (option) => {
    setSelectedFilterOptions((prev) => ({
      ...prev,
      [option]: !prev[option],
    }))
  }

  const handleSortClick = (sortId) => {
    setActiveSort(sortId)
    if (sortId === 'random') {
      // 递增随机种子，确保每次点击都生成新的随机顺序。
      setRandomSeed((prev) => prev + 1)
    }
  }

  const sortedItems = useMemo(() => {
    const source = [...items]

    if (activeSort === 'latest') {
      return source.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt))
    }
    if (activeSort === 'downloads') {
      return source.sort((a, b) => (b.views ?? 0) - (a.views ?? 0))
    }
    if (activeSort === 'likes') {
      return source.sort((a, b) => (b.likes ?? 0) - (a.likes ?? 0))
    }
    if (activeSort === 'boost') {
      return source.sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
    }
    if (activeSort === 'random') {
      // 基于种子的确定性随机排序，保证同一轮渲染稳定。
      return source.sort((a, b) => {
        const scoreA = ((a.id * 997 + randomSeed * 389) % 1000)
        const scoreB = ((b.id * 997 + randomSeed * 389) % 1000)
        return scoreA - scoreB
      })
    }

    // 默认“热门”排序：点赞数 + 浏览数。
    return source.sort((a, b) => ((b.likes ?? 0) + (b.views ?? 0)) - ((a.likes ?? 0) + (a.views ?? 0)))
  }, [items, activeSort, randomSeed])

  // 循环复用源数据，直到补齐 24 条。
  const boardItems = useMemo(() => {
    if (sortedItems.length === 0) return []
    return Array.from({ length: LASER_BOARD_TOTAL }, (_, index) => {
      const source = sortedItems[index % sortedItems.length]
      return {
        ...source,
        id: `${source.id}-laser-board-${index}`,
      }
    })
  }, [sortedItems])

  // 当数据或排序变化时重置可见数量，从首批重新加载。
  useEffect(() => {
    setVisibleCount(Math.min(LASER_BOARD_INITIAL, boardItems.length))
  }, [boardItems.length, activeSort, selectedTime, randomSeed])

  // 监听哨兵元素，在滚动时逐步展示更多卡片。
  useEffect(() => {
    if (visibleCount >= LASER_BOARD_TOTAL) return undefined
    const target = loadMoreSentinelRef.current
    if (!target) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        const isVisible = entries.some((entry) => entry.isIntersecting)
        if (!isVisible) return
        setVisibleCount((prev) => Math.min(prev + LASER_BOARD_BATCH, LASER_BOARD_TOTAL))
      },
      { root: null, rootMargin: '220px 0px' }
    )

    observer.observe(target)
    return () => observer.disconnect()
  }, [visibleCount])

  const visibleItems = boardItems.slice(0, Math.min(visibleCount, LASER_BOARD_TOTAL))
  const showLoader = visibleItems.length >= LASER_BOARD_TOTAL && visibleItems.length > 0

  return (
    <section className="px-6 pb-8 pt-2">
      {/* 工具栏：筛选开关与排序快捷入口 */}
      <div className="mb-5 flex items-center justify-between gap-3">
        <button
          className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700"
          onClick={() => setFiltersVisible((prev) => !prev)}
        >
          {filtersVisible ? (zh ? '隐藏筛选' : 'Hide Filters') : zh ? '显示筛选' : 'Show Filters'}
        </button>

        <div className="flex items-center gap-5 text-sm font-semibold text-slate-700">
          <div className="relative" ref={eventDropdownRef}>
            <button
              className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-3 py-1.5"
              onClick={() => setEventDropdownOpen((prev) => !prev)}
              type="button"
            >
              <span>{selectedEventLabel}</span>
              <span className="text-slate-500">▾</span>
            </button>

            {eventDropdownOpen ? (
              <div className="absolute right-0 top-[calc(100%+6px)] z-20 min-w-[132px] overflow-hidden rounded-md bg-white shadow-lg ring-1 ring-slate-200">
                {EVENT_OPTIONS.map((option) => {
                  const isActive = option.id === selectedEvent
                  return (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => {
                        setSelectedEvent(option.id)
                        setEventDropdownOpen(false)
                        handleSortClick(option.sortId)
                      }}
                      className={`block w-full px-4 py-2 text-left text-[14px] transition ${
                        isActive
                          ? 'bg-slate-100 font-semibold text-slate-900'
                          : 'text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      {zh ? option.zh : option.en}
                    </button>
                  )
                })}
              </div>
            ) : null}
          </div>

          {SORT_OPTIONS.map((option) => (
            <button
              key={option.id}
              onClick={() => handleSortClick(option.id)}
              className={`transition ${
                activeSort === option.id ? 'text-green-600' : 'text-slate-700 hover:text-slate-900'
              }`}
            >
              {zh ? option.zh : option.en}
            </button>
          ))}
          <div className="relative" ref={timeDropdownRef}>
            <button
              className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-white px-3 py-1.5"
              onClick={() => setTimeDropdownOpen((prev) => !prev)}
              type="button"
            >
              <span>{selectedTimeLabel}</span>
              <span className="text-slate-500">◷</span>
            </button>

            {timeDropdownOpen ? (
              <div className="absolute right-0 top-[calc(100%+6px)] z-20 min-w-[112px] overflow-hidden rounded-md bg-white shadow-lg ring-1 ring-slate-200">
                {TIME_OPTIONS.map((option) => {
                  const isActive = option.id === selectedTime
                  return (
                    <button
                      key={option.id}
                      type="button"
                      onClick={() => {
                        setSelectedTime(option.id)
                        setTimeDropdownOpen(false)
                      }}
                      className={`block w-full px-4 py-2 text-left text-[15px] transition ${
                        isActive
                          ? 'bg-slate-100 font-semibold text-slate-900'
                          : 'text-slate-700 hover:bg-slate-50'
                      }`}
                    >
                      {zh ? option.zh : option.en}
                    </button>
                  )
                })}
              </div>
            ) : null}
          </div>
        </div>
      </div>

      <div className={`grid grid-cols-1 items-start gap-5 ${filtersVisible ? 'xl:grid-cols-[270px_1fr]' : 'xl:grid-cols-1'}`}>
        {/* 左侧筛选面板 */}
        {filtersVisible ? (
          <aside className="h-fit self-start rounded-2xl bg-white p-5 ring-1 ring-slate-200">
            {FILTER_SECTIONS.map((section) => {
              const isOpen = openSections[section.title]

              return (
                <section
                  key={section.title}
                  className={`border-b border-slate-100 pt-2 last:border-none ${
                    isOpen ? 'pb-5' : 'pb-2'
                  }`}
                >
                  {/* 分组标题，支持展开/收起。 */}
                  <button
                    type="button"
                    onClick={() => toggleSection(section.title)}
                    className="mb-3 flex w-full items-center justify-between text-left"
                  >
                    <h4 className="text-[20px] font-semibold text-slate-900">{section.title}</h4>
                    <span
                      className={`text-sm font-semibold text-slate-500 transition-transform duration-200 ${
                        isOpen ? 'rotate-180' : 'rotate-0'
                      }`}
                    >
                      {'▾'}
                    </span>
                  </button>

                  {isOpen ? (
                    section.type === 'module-groups' ? (
                      <div className="space-y-3 text-[12px] text-slate-700">
                        {section.groups.map((group) => {
                          const groupOpen = openModuleGroups[group.id]
                          return (
                            <div key={group.id}>
                              <button
                                type="button"
                                onClick={() => toggleModuleGroup(group.id)}
                                className="flex w-full items-center justify-between rounded-md py-1 text-left"
                              >
                                <span className="flex items-center gap-2">
                                  <input
                                    type="checkbox"
                                    className="h-4 w-4 rounded border-slate-300"
                                    checked={Boolean(selectedFilterOptions[group.id])}
                                    onChange={(event) => {
                                      event.stopPropagation()
                                      toggleFilterOption(group.id)
                                    }}
                                  />
                                  <span className="font-semibold text-slate-800">{group.id}</span>
                                </span>
                                <span
                                  className={`text-xs text-slate-500 transition-transform duration-200 ${
                                    groupOpen ? 'rotate-180' : 'rotate-0'
                                  }`}
                                >
                                  {'▾'}
                                </span>
                              </button>

                              {groupOpen ? (
                                <div className="mt-2 rounded-md border border-slate-200 bg-slate-50 px-3 py-3">
                                  <div className="flex flex-wrap gap-4">
                                    {group.options.map((option) => (
                                      <label key={option} className="flex cursor-pointer items-center gap-2">
                                        <input
                                          type="checkbox"
                                          className="h-4 w-4 rounded border-slate-300"
                                          checked={Boolean(selectedFilterOptions[option])}
                                          onChange={() => toggleFilterOption(option)}
                                        />
                                        <span className="text-[12px]">{option}</span>
                                      </label>
                                    ))}
                                  </div>
                                </div>
                              ) : null}
                            </div>
                          )
                        })}
                      </div>
                    ) : (
                      <div className="space-y-3 text-[12px] text-slate-700">
                        {section.options.map((option) => (
                          <label key={option} className="flex cursor-pointer items-center gap-2">
                            <input
                              type="checkbox"
                              className="h-4 w-4 rounded border-slate-300"
                              checked={Boolean(selectedFilterOptions[option])}
                              onChange={() => toggleFilterOption(option)}
                            />
                            <span>{option}</span>
                          </label>
                        ))}
                      </div>
                    )
                  ) : null}
                </section>
              )
            })}
          </aside>
        ) : null}

        {/* 右侧卡片看板 */}
        <div className="grid grid-cols-1 gap-x-5 gap-y-7 sm:grid-cols-2 xl:grid-cols-4">
          {visibleItems.map((item) => (
            <LaserCard key={item.id} item={item} onOpen={onOpen} locale={locale} />
          ))}
        </div>
      </div>

      {visibleItems.length < LASER_BOARD_TOTAL ? (
        <div ref={loadMoreSentinelRef} className="h-6" />
      ) : null}

      {showLoader ? <LoadingState /> : null}
    </section>
  )
}
