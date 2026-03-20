function IconBase({ children, className = 'h-5 w-5' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      {children}
    </svg>
  )
}

export function IconMenuPanel({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <rect x="3" y="5" width="18" height="14" rx="2.2" />
      <path d="M9 5v14" />
    </IconBase>
  )
}

export function IconMenu({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h16" />
    </IconBase>
  )
}

export function IconSearch({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
    </IconBase>
  )
}

export function IconGlobe({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <circle cx="12" cy="12" r="9" />
      <path d="M3 12h18" />
      <path d="M12 3a13.5 13.5 0 0 0 0 18" />
      <path d="M12 3a13.5 13.5 0 0 1 0 18" />
    </IconBase>
  )
}

export function IconHome({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.8V20h14V9.8" />
      <path d="M9.5 20v-5h5v5" />
    </IconBase>
  )
}

export function IconBoxes({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <rect x="3" y="4" width="8" height="8" rx="1.5" />
      <rect x="13" y="4" width="8" height="8" rx="1.5" />
      <rect x="8" y="13" width="8" height="8" rx="1.5" />
    </IconBase>
  )
}

export function IconFlask({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M10 3h4" />
      <path d="M10 3v4l-5 8.5A3 3 0 0 0 7.6 20h8.8a3 3 0 0 0 2.6-4.5L14 7V3" />
      <path d="M8 13h8" />
    </IconBase>
  )
}

export function IconTrophy({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M8 4h8v3a4 4 0 0 1-8 0z" />
      <path d="M6 6H4a2 2 0 0 0 2 2" />
      <path d="M18 6h2a2 2 0 0 1-2 2" />
      <path d="M12 11v4" />
      <path d="M9 21h6" />
    </IconBase>
  )
}

export function IconCoins({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <ellipse cx="12" cy="8" rx="6" ry="2.5" />
      <path d="M6 8v6c0 1.4 2.7 2.5 6 2.5s6-1.1 6-2.5V8" />
      <path d="M4 18h16" />
    </IconBase>
  )
}

export function IconPackage({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="m3 8 9-5 9 5-9 5-9-5z" />
      <path d="M3 8v8l9 5 9-5V8" />
      <path d="M12 13v8" />
    </IconBase>
  )
}

export function IconUsers({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <circle cx="9" cy="8" r="3" />
      <circle cx="17" cy="10" r="2.4" />
      <path d="M4 20a5 5 0 0 1 10 0" />
      <path d="M14.2 20a3.8 3.8 0 0 1 5.8 0" />
    </IconBase>
  )
}

export function IconExternalLink({ className = 'h-4 w-4' }) {
  return (
    <IconBase className={className}>
      <path d="M14 5h5v5" />
      <path d="m10 14 9-9" />
      <path d="M19 13v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h5" />
    </IconBase>
  )
}

export function IconForum({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M4 5h16v11H8l-4 3V5z" />
      <path d="M8 10h8" />
      <path d="M8 13h5" />
    </IconBase>
  )
}

export function IconFacebook({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M14 4h2.5v3H15a1 1 0 0 0-1 1v2h2.5l-.4 3H14v7h-3v-7H8.8v-3H11V7.8A3.8 3.8 0 0 1 14.8 4z" />
    </IconBase>
  )
}

export function IconInstagram({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <rect x="4" y="4" width="16" height="16" rx="4" />
      <circle cx="12" cy="12" r="3.5" />
      <circle cx="17" cy="7" r="0.9" fill="currentColor" stroke="none" />
    </IconBase>
  )
}

export function IconYoutube({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <rect x="3.5" y="6.5" width="17" height="11" rx="3" />
      <path d="m10 9 5 3-5 3z" fill="currentColor" stroke="none" />
    </IconBase>
  )
}

export function IconXBrand({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M6 5h2l10 14h-2z" />
      <path d="m18 5-5 6" />
      <path d="m11 13-5 6" />
    </IconBase>
  )
}

export function IconTiktok({ className = 'h-5 w-5' }) {
  return (
    <IconBase className={className}>
      <path d="M12 6v8.2a2.7 2.7 0 1 1-2.3-2.7" />
      <path d="M12 6a4 4 0 0 0 4 4" />
    </IconBase>
  )
}

export function IconCubeBadge({ className = 'h-4 w-4' }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d="m12 3 7 4v10l-7 4-7-4V7z" fill="currentColor" opacity="0.2" />
      <path d="m12 3 7 4-7 4-7-4z" />
      <path d="M5 7v10l7 4 7-4V7" />
      <path d="M12 11v10" />
    </svg>
  )
}
