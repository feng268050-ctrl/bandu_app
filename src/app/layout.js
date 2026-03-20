import './globals.css'

export const metadata = {
  title: 'Maker World Clone',
  description: 'Next.js rebuilt interactive Maker World clone.',
}

export default function RootLayout({ children }) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  )
}
