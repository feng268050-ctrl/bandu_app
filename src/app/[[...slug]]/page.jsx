import { redirect } from 'next/navigation'
import { cookies } from 'next/headers'
import InteractiveMakerWorldPage from '../../views/InteractiveMakerWorldPage'
import {
  isSidebarCollapsedCookieValue,
  SIDEBAR_COLLAPSED_COOKIE,
} from '../../lib/sidebarPreferences'

const LOCALE_SET = new Set(['zh', 'en'])
const SEGMENT_ROUTE_MAP = {
  '': 'home',
  'laser-cut-models': 'all-models',
  contests: 'contest',
  community: 'community',
}

function normalizeRouteParams(rawSlug = []) {
  if (!Array.isArray(rawSlug) || rawSlug.length === 0) return null
  const [locale, routeSegment = ''] = rawSlug

  if (!LOCALE_SET.has(locale)) return null
  if (rawSlug.length > 2) return null
  if (!(routeSegment in SEGMENT_ROUTE_MAP)) return null

  return {
    locale,
    routeId: SEGMENT_ROUTE_MAP[routeSegment],
  }
}

export default async function RoutedPage({ params }) {
  const resolvedParams = await params
  const parsed = normalizeRouteParams(resolvedParams?.slug)
  if (!parsed) redirect('/zh')
  const cookieStore = await cookies()
  const initialSidebarCollapsed = isSidebarCollapsedCookieValue(
    cookieStore.get(SIDEBAR_COLLAPSED_COOKIE)?.value,
  )

  return (
    <InteractiveMakerWorldPage
      initialLocale={parsed.locale}
      initialRouteId={parsed.routeId}
      initialSidebarCollapsed={initialSidebarCollapsed}
    />
  )
}
