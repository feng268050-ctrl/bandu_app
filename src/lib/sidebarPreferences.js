export const SIDEBAR_COLLAPSED_COOKIE = 'maker_world_sidebar_collapsed'

const SIDEBAR_COOKIE_MAX_AGE = 60 * 60 * 24 * 365

export function isSidebarCollapsedCookieValue(value) {
  return value === '1'
}

export function buildSidebarCollapsedCookie(collapsed) {
  return [
    `${SIDEBAR_COLLAPSED_COOKIE}=${collapsed ? '1' : '0'}`,
    'Path=/',
    `Max-Age=${SIDEBAR_COOKIE_MAX_AGE}`,
    'SameSite=Lax',
  ].join('; ')
}
