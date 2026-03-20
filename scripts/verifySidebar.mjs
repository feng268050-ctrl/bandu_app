import { appendFileSync } from 'node:fs'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'
import { execFileSync, spawn } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import { chromium } from 'playwright-core'

const HOST = '127.0.0.1'
const PORT = process.env.SIDEBAR_VERIFY_PORT ?? '3100'
const BASE_URL = `http://${HOST}:${PORT}`
const SCREENSHOT_DIR = path.join(process.cwd(), 'artifacts', 'sidebar-verification')
const LOG_PATH = path.join(SCREENSHOT_DIR, 'verify.log')
const EXPECTED_ASPECT_RATIO = 192 / 187
const NAVIGATION_TIMEOUT = 30000
const ACTION_TIMEOUT = 10000
const EDGE_CANDIDATES = [
  process.env.PLAYWRIGHT_EDGE_EXECUTABLE,
  'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
].filter(Boolean)

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

function logStep(message) {
  const line = `${new Date().toISOString()} ${message}`
  console.log(line)
  appendFileSync(LOG_PATH, `${line}\n`)
}

async function pathExists(targetPath) {
  try {
    await fs.access(targetPath)
    return true
  } catch {
    return false
  }
}

async function findEdgeExecutable() {
  for (const candidate of EDGE_CANDIDATES) {
    if (await pathExists(candidate)) {
      return candidate
    }
  }

  throw new Error(
    'Microsoft Edge executable not found. Set PLAYWRIGHT_EDGE_EXECUTABLE to a local browser path and retry.',
  )
}

function stopServer(serverProcess) {
  if (!serverProcess || serverProcess.exitCode !== null) {
    return
  }

  if (process.platform === 'win32') {
    try {
      execFileSync('taskkill', ['/pid', String(serverProcess.pid), '/t', '/f'], { stdio: 'ignore' })
    } catch {
      serverProcess.kill()
    }
    return
  }

  serverProcess.kill('SIGTERM')
}

async function waitForServer(serverProcess, getLogs) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    if (serverProcess.exitCode !== null) {
      throw new Error(`Verification server exited early.\n\n${getLogs()}`)
    }

    try {
      const response = await fetch(`${BASE_URL}/zh`, { redirect: 'manual' })
      if (response.ok || (response.status >= 300 && response.status < 400)) {
        return
      }
    } catch {
      // Ignore connection errors while the server is starting.
    }

    await delay(1000)
  }

  throw new Error(`Timed out waiting for ${BASE_URL}.\n\n${getLogs()}`)
}

async function startServer() {
  let logs = ''
  logStep(`Starting verification server on ${BASE_URL}`)
  const serverProcess = process.platform === 'win32'
    ? spawn('cmd.exe', ['/d', '/s', '/c', `npm.cmd run dev -- -H ${HOST} -p ${PORT}`], {
        cwd: process.cwd(),
        env: process.env,
        stdio: ['ignore', 'pipe', 'pipe'],
      })
    : spawn('npm', ['run', 'dev', '--', '-H', HOST, '-p', PORT], {
        cwd: process.cwd(),
        env: process.env,
        stdio: ['ignore', 'pipe', 'pipe'],
      })

  serverProcess.stdout.on('data', (chunk) => {
    const text = chunk.toString()
    logs += text
    process.stdout.write(text)
  })

  serverProcess.stderr.on('data', (chunk) => {
    const text = chunk.toString()
    logs += text
    process.stderr.write(text)
  })

  await waitForServer(serverProcess, () => logs)
  logStep('Verification server is ready')

  return {
    process: serverProcess,
    getLogs: () => logs,
  }
}

async function getSidebarWidth(page) {
  await page.getByTestId('app-sidebar').waitFor()

  return page.getByTestId('app-sidebar').evaluate((element) => {
    return Math.round(element.getBoundingClientRect().width)
  })
}

async function expectSidebarWidth(page, assertionLabel, predicate) {
  const width = await getSidebarWidth(page)
  assert(predicate(width), `${assertionLabel}. Observed sidebar width: ${width}px`)
  return width
}

async function toggleSidebarUntil(page, assertionLabel, predicate) {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    await page.getByTestId('sidebar-toggle').click()
    await delay(300)

    const width = await getSidebarWidth(page)
    if (predicate(width)) {
      return width
    }

    await delay(500)
  }

  return expectSidebarWidth(page, assertionLabel, predicate)
}

async function clickSidebarItem(page, testId, expectedPath) {
  await Promise.all([
    page.waitForURL((url) => url.pathname === expectedPath, { timeout: NAVIGATION_TIMEOUT }),
    page.getByTestId(testId).click(),
  ])
}

async function verifyCollapsedStatePersistence(page) {
  logStep('Checking collapsed-state persistence')
  await page.goto(`${BASE_URL}/zh`, { waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT })
  await page.getByTestId('sidebar-toggle').waitFor()

  const collapsedWidth = await toggleSidebarUntil(
    page,
    'Sidebar should collapse to the narrow desktop rail',
    (width) => width <= 90,
  )

  await clickSidebarItem(page, 'sidebar-item-contest', '/zh/contests')
  await expectSidebarWidth(
    page,
    'Sidebar should stay collapsed after navigating to Contest',
    (width) => Math.abs(width - collapsedWidth) <= 6,
  )

  await clickSidebarItem(page, 'sidebar-item-community', '/zh/community')
  await expectSidebarWidth(
    page,
    'Sidebar should stay collapsed after navigating to Community',
    (width) => Math.abs(width - collapsedWidth) <= 6,
  )

  const expandedWidth = await toggleSidebarUntil(
    page,
    'Sidebar should return to the expanded desktop width',
    (width) => width >= 220,
  )

  await clickSidebarItem(page, 'sidebar-item-home', '/zh')
  await expectSidebarWidth(
    page,
    'Sidebar should stay expanded after internal navigation',
    (width) => Math.abs(width - expandedWidth) <= 6,
  )

  return { collapsedWidth, expandedWidth }
}

async function collectPreviewMetrics(page, locale) {
  logStep(`Checking preview layout for locale "${locale}"`)
  await page.goto(`${BASE_URL}/${locale}/community`, { waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT })
  await page.getByTestId('community-activity-preview-image-frame').waitFor()

  const metrics = await page.evaluate(() => {
    const frame = document.querySelector('[data-testid="community-activity-preview-image-frame"]')
    const card = frame?.closest('section')
    const content = document.querySelector('[data-testid="community-activity-preview-content"]')
    const title = document.querySelector('[data-testid="community-activity-preview-title"]')

    if (!card || !content || !frame || !title) {
      return null
    }

    const cardRect = card.getBoundingClientRect()
    const contentRect = content.getBoundingClientRect()
    const contentStyle = window.getComputedStyle(content)
    const contentInnerWidth = content.clientWidth
      - Number.parseFloat(contentStyle.paddingLeft)
      - Number.parseFloat(contentStyle.paddingRight)
    const frameRect = frame.getBoundingClientRect()
    const titleRect = title.getBoundingClientRect()

    return {
      cardWidth: cardRect.width,
      cardScrollWidth: card.scrollWidth,
      contentWidth: contentRect.width,
      contentInnerWidth,
      contentScrollWidth: content.scrollWidth,
      frameWidth: frameRect.width,
      frameHeight: frameRect.height,
      frameToContentRatio: frameRect.width / contentInnerWidth,
      frameAspectRatio: frameRect.width / frameRect.height,
      titleRightInset: contentRect.right - titleRect.right,
      titleLeftInset: titleRect.left - contentRect.left,
      titleScrollWidth: title.scrollWidth,
      titleClientWidth: title.clientWidth,
    }
  })

  assert(metrics, `Preview metrics for locale "${locale}" could not be collected`)
  assert(
    metrics.frameToContentRatio >= 0.63 && metrics.frameToContentRatio <= 0.67,
    `Preview image ratio for locale "${locale}" should remain near 65%. Observed ${metrics.frameToContentRatio.toFixed(3)}.`,
  )
  assert(
    Math.abs(metrics.frameAspectRatio - EXPECTED_ASPECT_RATIO) <= 0.03,
    `Preview image aspect ratio for locale "${locale}" drifted. Observed ${metrics.frameAspectRatio.toFixed(3)}.`,
  )
  assert(
    metrics.cardScrollWidth <= metrics.cardWidth + 1,
    `Preview card overflows horizontally in locale "${locale}".`,
  )
  assert(
    metrics.contentScrollWidth <= metrics.contentWidth + 1,
    `Preview content overflows horizontally in locale "${locale}".`,
  )
  assert(
    metrics.titleScrollWidth <= metrics.titleClientWidth + 1,
    `Preview title overflows horizontally in locale "${locale}".`,
  )
  assert(
    metrics.titleLeftInset >= 0 && metrics.titleRightInset >= 0,
    `Preview title is misaligned outside its content bounds in locale "${locale}".`,
  )

  const screenshotPath = path.join(SCREENSHOT_DIR, `community-preview-${locale}.png`)
  await page
    .getByTestId('community-activity-preview-image-frame')
    .locator('xpath=ancestor::section[1]')
    .screenshot({ path: screenshotPath })

  return {
    ...metrics,
    screenshotPath,
  }
}

async function main() {
  await fs.mkdir(SCREENSHOT_DIR, { recursive: true })
  await fs.writeFile(LOG_PATH, '')

  const edgeExecutable = await findEdgeExecutable()
  logStep(`Launching Edge from ${edgeExecutable}`)
  const server = await startServer()
  const browser = await chromium.launch({
    executablePath: edgeExecutable,
    headless: true,
    timeout: ACTION_TIMEOUT,
  })

  try {
    const page = await browser.newPage({
      viewport: { width: 1440, height: 1200 },
    })
    page.setDefaultTimeout(ACTION_TIMEOUT)

    const sidebarMetrics = await verifyCollapsedStatePersistence(page)
    const zhPreviewMetrics = await collectPreviewMetrics(page, 'zh')
    const enPreviewMetrics = await collectPreviewMetrics(page, 'en')

    logStep('Sidebar verification passed')
    console.log(JSON.stringify({
      sidebarMetrics,
      zhPreviewMetrics,
      enPreviewMetrics,
    }, null, 2))
  } finally {
    await browser.close()
    stopServer(server.process)
  }
}

main().catch((error) => {
  logStep('Sidebar verification failed')
  console.error(error instanceof Error ? error.stack : error)
  process.exitCode = 1
})
