import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { categories } from '../data/mockData'

export default function CategoryTabs({ activeCategory, setActiveCategory, labelMap, inline = false }) {
  const controlled = typeof setActiveCategory === 'function' && typeof activeCategory === 'string'
  const [expanded, setExpanded] = useState(false)
  const [hasOverflow, setHasOverflow] = useState(false)
  const [arrowLeft, setArrowLeft] = useState(0)
  const wrapperRef = useRef(null)
  const tabsRef = useRef(null)
  const arrowRef = useRef(null)

  useEffect(() => {
    if (!inline || expanded) return undefined

    const checkOverflow = () => {
      const element = tabsRef.current
      if (!element) return
      setHasOverflow(element.scrollWidth > element.clientWidth + 1)
    }

    checkOverflow()
    window.addEventListener('resize', checkOverflow)
    return () => window.removeEventListener('resize', checkOverflow)
  }, [inline, expanded, labelMap])

  const updateArrowPosition = useCallback(() => {
    if (!inline) return
    const wrapper = wrapperRef.current
    const tabs = tabsRef.current
    const arrow = arrowRef.current
    if (!wrapper || !arrow) return

    const wrapperWidth = wrapper.clientWidth
    const arrowWidth = arrow.offsetWidth || 40
    const maxLeft = Math.max(0, wrapperWidth - arrowWidth)

    if (!expanded || !tabs) {
      setArrowLeft(maxLeft)
      return
    }

    const tabButtons = Array.from(tabs.querySelectorAll('button'))
    if (tabButtons.length === 0) {
      setArrowLeft(maxLeft)
      return
    }

    const firstRowTop = tabButtons[0].offsetTop
    const firstRowItems = tabButtons.filter((button) => Math.abs(button.offsetTop - firstRowTop) <= 2)
    const lastInFirstRow = firstRowItems[firstRowItems.length - 1]
    const desiredLeft = lastInFirstRow.offsetLeft + lastInFirstRow.offsetWidth + 8
    setArrowLeft(Math.min(maxLeft, Math.max(0, desiredLeft)))
  }, [expanded, inline])

  useLayoutEffect(() => {
    if (!inline) return undefined

    const frameId = window.requestAnimationFrame(updateArrowPosition)
    const handleResize = () => window.requestAnimationFrame(updateArrowPosition)
    window.addEventListener('resize', handleResize)

    return () => {
      window.cancelAnimationFrame(frameId)
      window.removeEventListener('resize', handleResize)
    }
  }, [inline, expanded, hasOverflow, labelMap, activeCategory, updateArrowPosition])

  const formatTabLabel = (label) => {
    if (typeof label !== 'string') return label
    return /3D/i.test(label) ? 'CNC' : label
  }

  const tabs = (
    <div
      ref={tabsRef}
      className={`hide-scrollbar flex gap-3 ${
        inline
          ? expanded
            ? 'flex-wrap overflow-visible'
            : 'flex-nowrap overflow-hidden'
          : 'overflow-x-auto pb-1'
      }`}
    >
      {categories.map((item, index) => {
        const isActive = controlled ? activeCategory === item : index === 0
        return (
          <button
            key={item}
            onClick={controlled ? () => setActiveCategory(item) : undefined}
            className={`whitespace-nowrap rounded-xl px-4 py-2 text-[15px] font-semibold transition ${
              isActive
                ? 'bg-slate-900 text-white'
                : 'bg-slate-200/80 text-slate-700 hover:bg-slate-300/70'
            }`}
          >
            {formatTabLabel(labelMap?.[item] ?? item)}
          </button>
        )
      })}
    </div>
  )

  if (inline) {
    const showToggle = hasOverflow || expanded

    return (
      <div ref={wrapperRef} className="relative min-w-0">
        <div className="pr-12">{tabs}</div>
        {showToggle ? (
          <button
            ref={arrowRef}
            type="button"
            onClick={() => setExpanded((prev) => !prev)}
            aria-expanded={expanded}
            aria-label={expanded ? '收起分类' : '展开分类'}
            style={{ left: `${arrowLeft}px` }}
            className="absolute top-0 z-10 flex h-10 w-10 items-center justify-center rounded-xl bg-slate-200/80 text-slate-700 transition-[left,background-color] duration-300 hover:bg-slate-300/70"
          >
            <span
              className={`inline-block text-base font-semibold leading-none transition-transform duration-300 ${
                expanded ? 'rotate-180' : 'rotate-0'
              }`}
            >
              {'▾'}
            </span>
          </button>
        ) : null}
      </div>
    )
  }

  return <section className="px-6 pb-4">{tabs}</section>
}
