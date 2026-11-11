import { randomUUID } from 'crypto'
import { mkdirSync } from 'fs'
import { basename, join } from 'path'
import {
  chromium,
  type BrowserContext,
  type Download,
  type Locator,
  type Page
} from 'playwright-core'
import { normalizeWebUrl, safeWebPageUrl } from './web-url'

export type BrowserAction =
  | 'open'
  | 'snapshot'
  | 'click'
  | 'fill'
  | 'select'
  | 'check'
  | 'uncheck'
  | 'press'
  | 'hover'
  | 'scroll'
  | 'upload'
  | 'extract'
  | 'screenshot'
  | 'wait'
  | 'back'
  | 'reload'
  | 'pages'
  | 'switch'
  | 'close_tab'
  | 'perform'
  | 'close'

export interface BrowserOperationRequest {
  userId?: string | number
  projectId?: string
  conversationId?: string
  action: BrowserAction
  args?: Record<string, unknown>
}

export interface BrowserOperationResult {
  text: string
  url: string
  title: string
  screenshot?: string
}

interface BrowserSession {
  page: Page | null
  pages: Set<Page>
  dialogs: string[]
  downloads: string[]
}

interface SnapshotNode {
  ref: string
  role: string
  name: string
  value: string
  type: string
  checked: boolean | null
  disabled: boolean
  required: boolean
  expanded: boolean | null
  options: string
  href: string
}

interface PageSnapshot {
  title: string
  url: string
  nodes: SnapshotNode[]
  headings: string[]
  notices: string[]
  text: string
  iframeCount: number
}

const CANDIDATE_SELECTOR = [
  'a',
  'button',
  'input',
  'select',
  'textarea',
  'summary',
  '[role="button"]',
  '[role="link"]',
  '[role="checkbox"]',
  '[role="radio"]',
  '[role="switch"]',
  '[role="combobox"]',
  '[role="textbox"]',
  '[role="searchbox"]',
  '[role="tab"]',
  '[role="menuitem"]',
  '[contenteditable="true"]',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

const MAX_STEPS = 30
const DEFAULT_TIMEOUT = 15_000
const NAVIGATION_TIMEOUT = 30_000

/** 页面标题 / URL / 弹窗与下载都归一成一段模型可读的文本，避免 IPC 里堆结构化冗余。 */
function titleOf(page: Page): string {
  return page.url() === 'about:blank' ? '（空白页）' : ''
}

function safeName(value: unknown): string {
  return (
    String(value ?? '')
      // eslint-disable-next-line no-control-regex -- Windows 文件名不允许控制字符
      .replace(/[<>:"/\\|?*\u0000-\u001f]/g, '_')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 120)
  )
}

function clip(value: unknown, max = 500): string {
  const text = String(value ?? '')
  return text.length > max ? `${text.slice(0, max)}…` : text
}

/**
 * 浏览器自动化执行器。
 *
 * 一个会话复用一个标签页；用户账号共用一个持久化浏览器目录，因此登录态会保留。
 * 页面结构快照会给每个可操作控件分配稳定到下一次快照为止的 ref，模型只按 ref 操作。
 * 视觉截图是补充信息，不作为第一版的默认执行依据。
 */
export class BrowserAutomation {
  private root = ''
  private context: BrowserContext | null = null
  private profileKey = ''
  private sessions = new Map<string, BrowserSession>()
  private attachedPages = new WeakSet<Page>()
  private closing = false
  private closeCount = 0
  private pendingLaunches = new Set<Promise<BrowserContext>>()
  private pendingContextClosures = new Set<Promise<void>>()

  private get accepting(): boolean {
    return !this.closing && this.closeCount === 0
  }

  private assertAccepting(): void {
    if (this.closing) throw new Error('客户端正在退出，无法继续浏览器操作')
    if (this.closeCount > 0) throw new Error('浏览器正在关闭，请稍后重试')
  }

  init(runtimeDir: string): void {
    this.root = join(runtimeDir, 'userdata', 'automation')
    mkdirSync(this.profilesRoot, { recursive: true })
    mkdirSync(this.tempRoot, { recursive: true })
  }

  private get profilesRoot(): string {
    return join(this.root, 'profiles')
  }

  private get tempRoot(): string {
    return join(this.root, 'temp')
  }

  private profileDir(userId: unknown): string {
    return join(this.profilesRoot, safeName(userId) || 'shared')
  }

  /** 让 Playwright 使用机器上已安装的 Edge / Chrome，避免第一版额外下载一套 Chromium。 */
  private async ensureContext(userId: unknown): Promise<BrowserContext> {
    this.assertAccepting()
    const nextProfile = this.profileDir(userId)
    if (this.context && nextProfile === this.profileKey) return this.context
    if (this.context) {
      const previous = this.context
      this.context = null
      this.profileKey = ''
      const closing = previous.close().catch(() => undefined)
      this.pendingContextClosures.add(closing)
      try {
        await closing
      } finally {
        this.pendingContextClosures.delete(closing)
      }
    }

    const failures: string[] = []
    for (const channel of ['msedge', 'chrome']) {
      this.assertAccepting()
      const launch = (async () => {
        const context = await chromium.launchPersistentContext(nextProfile, {
          channel,
          headless: false,
          viewport: null,
          acceptDownloads: true,
          locale: 'zh-CN',
          args: [
            '--start-maximized',
            '--no-first-run',
            '--disable-blink-features=AutomationControlled'
          ]
        })
        // 启动窗口可能和退出清理并发：清理先到就把刚启动的浏览器立即关掉，
        // 不能让 closeAll 只等待 launch、却不等它随后的关闭完成。
        if (!this.accepting) {
          await context.close().catch(() => undefined)
          if (this.closing) throw new Error('客户端正在退出，无法启动浏览器操作')
          throw new Error('浏览器正在关闭，请稍后重试')
        }
        return context
      })()
      this.pendingLaunches.add(launch)
      try {
        const context = await launch
        context.setDefaultTimeout(DEFAULT_TIMEOUT)
        context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT)
        context.on('close', () => {
          if (this.context === context) {
            this.context = null
            this.profileKey = ''
          }
        })
        this.context = context
        this.profileKey = nextProfile
        return context
      } catch (error) {
        failures.push(`${channel}: ${error instanceof Error ? error.message : String(error)}`)
      } finally {
        this.pendingLaunches.delete(launch)
      }
    }
    throw new Error(
      `无法启动浏览器，请确认电脑已安装 Microsoft Edge 或 Google Chrome。\n${failures.join('\n')}`
    )
  }

  private async sessionFor(
    userId: unknown,
    conversationId: unknown
  ): Promise<{ session: BrowserSession; page: Page }> {
    const context = await this.ensureContext(userId)
    const key = String(conversationId ?? '').trim() || 'default'
    let session = this.sessions.get(key)
    if (!session) {
      session = { page: null, pages: new Set(), dialogs: [], downloads: [] }
      this.sessions.set(key, session)
    }
    if (!session.page || session.page.isClosed()) {
      session.page = await context.newPage()
      await this.attachPage(session, session.page)
    }
    return { session, page: session.page }
  }

  private async attachPage(session: BrowserSession, page: Page): Promise<void> {
    session.pages.add(page)
    if (this.attachedPages.has(page)) return
    this.attachedPages.add(page)
    page.on('dialog', (dialog) => {
      const message = `${dialog.type()}: ${clip(dialog.message(), 300)}`
      session.dialogs.push(message)
      this.trimEvents(session)
      // 第一版以少打扰为目标：网页 alert / confirm / beforeunload 默认接受。
      void dialog.accept().catch(() => undefined)
    })
    page.on('download', (download) => {
      void this.saveDownload(session, download)
    })
    page.on('popup', (popup) => {
      session.page = popup
      void this.attachPage(session, popup)
    })
  }

  private trimEvents(session: BrowserSession): void {
    if (session.dialogs.length > 10) session.dialogs.splice(0, session.dialogs.length - 10)
    if (session.downloads.length > 10) session.downloads.splice(0, session.downloads.length - 10)
  }

  private async saveDownload(session: BrowserSession, download: Download): Promise<void> {
    try {
      const name = safeName(basename(download.suggestedFilename())) || `download-${randomUUID()}`
      const path = join(this.tempRoot, 'downloads', `${Date.now()}-${name}`)
      mkdirSync(join(this.tempRoot, 'downloads'), { recursive: true })
      await download.saveAs(path)
      session.downloads.push(path)
      this.trimEvents(session)
    } catch (error) {
      session.dialogs.push(
        `下载保存失败：${error instanceof Error ? error.message : String(error)}`
      )
      this.trimEvents(session)
    }
  }

  private async settle(page: Page): Promise<void> {
    if (page.isClosed()) return
    await page.waitForTimeout(250)
    await page.waitForLoadState('domcontentloaded', { timeout: 3_000 }).catch(() => undefined)
    await page.waitForLoadState('networkidle', { timeout: 1_500 }).catch(() => undefined)
  }

  private async assertSessionPagesSafe(session: BrowserSession): Promise<void> {
    const unsafe = [...session.pages].filter(
      (item) => !item.isClosed() && !safeWebPageUrl(item.url())
    )
    if (!unsafe.length) return
    for (const item of unsafe) {
      await item.close().catch(() => undefined)
      session.pages.delete(item)
      if (session.page === item) session.page = null
    }
    throw new Error('Browser Agent 不允许打开非 http / https 页面；越界页面已关闭')
  }

  private async locatorFor(page: Page, args: Record<string, unknown>): Promise<Locator> {
    const ref = String(args.ref ?? '').trim()
    if (ref) {
      if (!/^e\d+$/.test(ref)) throw new Error(`ref 格式不正确：${ref}`)
      const locator = page.locator(`[data-fsbot-ref="${ref}"]`).first()
      if ((await locator.count()) < 1) {
        throw new Error(`找不到控件 ${ref}，页面可能已变化；请重新执行 snapshot 获取新 ref`)
      }
      return locator
    }
    const selector = String(args.selector ?? '').trim()
    if (selector) return page.locator(selector).first()
    const label = String(args.label ?? '').trim()
    if (label) return page.getByLabel(label, { exact: false }).first()
    const placeholder = String(args.placeholder ?? '').trim()
    if (placeholder) return page.getByPlaceholder(placeholder, { exact: false }).first()
    const text = String(args.text ?? '').trim()
    if (text) return page.getByText(text, { exact: false }).first()
    throw new Error('这个动作需要 ref；也可以提供 selector、label、placeholder 或 text')
  }

  private async snapshot(page: Page, maxElements = 140): Promise<PageSnapshot> {
    if (page.isClosed()) throw new Error('浏览器标签页已经关闭，请先执行 open')
    return page.evaluate(
      ({ selector, limit }) => {
        const clean = (value: unknown): string =>
          String(value ?? '')
            .replace(/\s+/g, ' ')
            .trim()
        const cut = (value: unknown, max = 240): string => {
          const text = clean(value)
          return text.length > max ? `${text.slice(0, max)}…` : text
        }
        const visible = (element: Element): boolean => {
          const node = element as HTMLElement
          const style = window.getComputedStyle(node)
          const rect = node.getBoundingClientRect()
          return (
            style.display !== 'none' &&
            style.visibility !== 'hidden' &&
            Number(style.opacity || 1) > 0 &&
            rect.width > 0 &&
            rect.height > 0
          )
        }
        const roleOf = (element: Element): string => {
          const explicit = element.getAttribute('role')
          if (explicit) return explicit
          const tag = element.tagName.toLowerCase()
          const type = String((element as HTMLInputElement).type ?? '').toLowerCase()
          if (tag === 'a') return 'link'
          if (tag === 'button' || (tag === 'input' && ['button', 'submit', 'reset'].includes(type)))
            return 'button'
          if (tag === 'select') return 'combobox'
          if (tag === 'textarea') return 'textbox'
          if (tag === 'summary') return 'button'
          if (tag === 'input') {
            if (type === 'checkbox') return 'checkbox'
            if (type === 'radio') return 'radio'
            if (type === 'range') return 'slider'
            if (type === 'search') return 'searchbox'
            return 'textbox'
          }
          if ((element as HTMLElement).isContentEditable) return 'textbox'
          return tag
        }
        const labelOf = (element: Element): string => {
          const input = element as HTMLInputElement
          const labels = input.labels ? Array.from(input.labels) : []
          const labelledBy = String(element.getAttribute('aria-labelledby') ?? '')
            .split(/\s+/)
            .map((id) => document.getElementById(id)?.textContent ?? '')
            .join(' ')
          return cut(
            element.getAttribute('aria-label') ||
              labelledBy ||
              labels.map((item) => item.textContent ?? '').join(' ') ||
              element.getAttribute('placeholder') ||
              element.getAttribute('alt') ||
              element.getAttribute('title') ||
              element.textContent ||
              input.value ||
              element.getAttribute('name') ||
              ''
          )
        }

        for (const old of Array.from(document.querySelectorAll('[data-fsbot-ref]'))) {
          old.removeAttribute('data-fsbot-ref')
        }
        const nodes: Array<{
          ref: string
          role: string
          name: string
          value: string
          type: string
          checked: boolean | null
          disabled: boolean
          required: boolean
          expanded: boolean | null
          options: string
          href: string
        }> = []
        const seen = new Set<Element>()
        for (const element of Array.from(document.querySelectorAll(selector))) {
          if (seen.has(element) || !visible(element)) continue
          const input = element as HTMLInputElement
          const tag = element.tagName.toLowerCase()
          const type = String(input.type ?? '').toLowerCase()
          if (tag === 'input' && type === 'hidden') continue
          seen.add(element)
          const ref = `e${nodes.length + 1}`
          element.setAttribute('data-fsbot-ref', ref)
          const options =
            tag === 'select'
              ? Array.from((element as HTMLSelectElement).options)
                  .slice(0, 40)
                  .map(
                    (option) =>
                      `${option.selected ? '*' : ''}${cut(option.text, 80)}${
                        option.value !== option.text ? `[${cut(option.value, 80)}]` : ''
                      }`
                  )
                  .join(' | ')
              : ''
          nodes.push({
            ref,
            role: roleOf(element),
            name: labelOf(element),
            value:
              tag === 'select'
                ? cut((element as HTMLSelectElement).selectedOptions[0]?.text ?? '')
                : cut(input.value ?? ''),
            type,
            checked:
              type === 'checkbox' || type === 'radio'
                ? !!input.checked
                : element.getAttribute('aria-checked') === null
                  ? null
                  : element.getAttribute('aria-checked') === 'true',
            disabled:
              !!input.disabled ||
              !!element.getAttribute('aria-disabled')?.match(/^true$/i) ||
              (element as HTMLButtonElement).disabled === true,
            required:
              !!input.required ||
              !!element.getAttribute('aria-required')?.match(/^true$/i) ||
              !!element.getAttribute('required'),
            expanded:
              element.getAttribute('aria-expanded') === null
                ? null
                : element.getAttribute('aria-expanded') === 'true',
            options,
            href: cut((element as HTMLAnchorElement).href ?? '', 300)
          })
          if (nodes.length >= limit) break
        }
        const headings = Array.from(document.querySelectorAll('h1,h2,h3'))
          .filter(visible)
          .slice(0, 30)
          .map((item) => cut(item.textContent, 180))
          .filter(Boolean)
        const notices = Array.from(
          document.querySelectorAll(
            '[role="alert"],[aria-live="assertive"],[aria-live="polite"],.el-message,.ant-message,.toast'
          )
        )
          .filter(visible)
          .slice(0, 20)
          .map((item) => cut(item.textContent, 300))
          .filter(Boolean)
        const bodyText = cut(document.body?.innerText ?? '', 7000)
        return {
          title: document.title,
          url: location.href,
          nodes,
          headings,
          notices,
          text: bodyText,
          iframeCount: document.querySelectorAll('iframe,frame').length
        }
      },
      { selector: CANDIDATE_SELECTOR, limit: Math.max(20, Math.min(220, maxElements)) }
    )
  }

  private formatSnapshot(snapshot: PageSnapshot, session: BrowserSession, page: Page): string {
    const pages = [...session.pages].filter((item) => !item.isClosed())
    const lines = [
      `【浏览器】${snapshot.title || titleOf(page)}`,
      `URL：${snapshot.url}`,
      `标签页：${Math.max(1, pages.indexOf(page) + 1)} / ${pages.length}`
    ]
    if (session.dialogs.length) {
      lines.push(`最近弹窗：${session.dialogs.slice(-3).join('；')}`)
    }
    if (session.downloads.length) {
      lines.push(`最近下载：${session.downloads.slice(-3).join('；')}`)
    }
    if (snapshot.headings.length) {
      lines.push(`页面标题：${snapshot.headings.join(' / ')}`)
    }
    if (snapshot.notices.length) {
      lines.push(`页面提示：${snapshot.notices.join(' / ')}`)
    }
    lines.push('', '可操作控件（下一步用 ref）：')
    if (!snapshot.nodes.length) {
      lines.push('（没有发现可操作控件，可以读取正文或截图）')
    } else {
      for (const node of snapshot.nodes) {
        const state: string[] = []
        if (node.type) state.push(`type=${node.type}`)
        if (node.checked !== null) state.push(node.checked ? 'checked' : 'unchecked')
        if (node.disabled) state.push('disabled')
        if (node.required) state.push('required')
        if (node.expanded !== null) state.push(node.expanded ? 'expanded' : 'collapsed')
        if (node.options) state.push(`options=${node.options}`)
        if (node.href && node.href !== 'http:///' && node.href !== 'https:///') {
          state.push(`href=${node.href}`)
        }
        const value = node.value ? ` value="${clip(node.value, 180)}"` : ''
        lines.push(
          `${node.ref} ${node.role} "${clip(node.name, 220)}"${value}${
            state.length ? ` (${state.join(', ')})` : ''
          }`
        )
      }
    }
    if (snapshot.iframeCount) {
      lines.push('', `注意：页面有 ${snapshot.iframeCount} 个 iframe；第一版只扫描主页面。`)
    }
    if (snapshot.text) {
      lines.push('', '页面可见文字（截断）：', snapshot.text)
    }
    return lines.join('\n')
  }

  private async result(
    page: Page,
    session: BrowserSession,
    text: string,
    screenshot?: string
  ): Promise<BrowserOperationResult> {
    const context = this.context
    const title = page.isClosed() ? '' : await page.title().catch(() => '')
    return {
      text:
        context && !page.isClosed()
          ? `${text}\n\n${this.formatSnapshot(
              await this.snapshot(page, 80).catch(() => ({
                title,
                url: page.url(),
                nodes: [],
                headings: [],
                notices: [],
                text: '',
                iframeCount: 0
              })),
              session,
              page
            )}`
          : text,
      url: page.isClosed() ? '' : page.url(),
      title: title || titleOf(page),
      ...(screenshot ? { screenshot } : {})
    }
  }

  private async performStep(
    page: Page,
    action: string,
    args: Record<string, unknown>
  ): Promise<string> {
    if (action === 'click') {
      const target = await this.locatorFor(page, args)
      await target.scrollIntoViewIfNeeded().catch(() => undefined)
      await target.click({ timeout: DEFAULT_TIMEOUT })
      await this.settle(page)
      return '点击完成'
    }
    if (action === 'fill') {
      const target = await this.locatorFor(page, args)
      const value = String(args.value ?? '')
      await target.fill(value, { timeout: DEFAULT_TIMEOUT })
      return `已填写「${clip(args.ref ?? args.label ?? args.placeholder ?? args.text, 50)}」`
    }
    if (action === 'select') {
      const target = await this.locatorFor(page, args)
      const option = String(args.value ?? args.label ?? args.text ?? '')
      if (!option) throw new Error('select 需要 value 或 label')
      await target.selectOption(
        args.value !== undefined ? String(args.value) : { label: String(args.label ?? args.text) }
      )
      return `已选择「${option}」`
    }
    if (action === 'check' || action === 'uncheck') {
      const target = await this.locatorFor(page, args)
      if (action === 'check') await target.check({ timeout: DEFAULT_TIMEOUT })
      else await target.uncheck({ timeout: DEFAULT_TIMEOUT })
      return action === 'check' ? '已勾选' : '已取消勾选'
    }
    if (action === 'press') {
      const key = String(args.key ?? '').trim()
      if (!key) throw new Error('press 需要 key，例如 Enter、Tab、Escape')
      if (args.ref || args.selector || args.label || args.placeholder || args.text) {
        const target = await this.locatorFor(page, args)
        await target.press(key, { timeout: DEFAULT_TIMEOUT })
      } else {
        await page.keyboard.press(key)
      }
      await this.settle(page)
      return `已按键 ${key}`
    }
    if (action === 'hover') {
      const target = await this.locatorFor(page, args)
      await target.hover({ timeout: DEFAULT_TIMEOUT })
      await page.waitForTimeout(150)
      return '悬停完成'
    }
    if (action === 'scroll') {
      const x = Math.max(-10_000, Math.min(10_000, Number(args.x ?? 0) || 0))
      const y = Math.max(-20_000, Math.min(20_000, Number(args.y ?? 700) || 0))
      await page.evaluate(({ x, y }) => window.scrollBy(x, y), { x, y })
      await page.waitForTimeout(200)
      return `已滚动 (${x}, ${y})`
    }
    if (action === 'upload') {
      const target = await this.locatorFor(page, args)
      const path = String(args.path ?? '').trim()
      if (!path) throw new Error('upload 需要 path（本机文件绝对路径）')
      await target.setInputFiles(path, { timeout: DEFAULT_TIMEOUT })
      return `已选择文件 ${path}`
    }
    if (action === 'wait') {
      const target = args.ref || args.selector || args.label || args.placeholder || args.text
      if (target) {
        await (
          await this.locatorFor(page, args)
        ).waitFor({
          state: 'visible',
          timeout: DEFAULT_TIMEOUT
        })
        return '控件已出现'
      }
      const ms = Math.max(0, Math.min(20_000, Number(args.ms ?? 1000) || 1000))
      await page.waitForTimeout(ms)
      return `已等待 ${ms}ms`
    }
    throw new Error(`perform 不支持动作：${action}`)
  }

  private async execute(
    request: BrowserOperationRequest,
    page: Page,
    session: BrowserSession,
    context: BrowserContext
  ): Promise<BrowserOperationResult> {
    const args = request.args ?? {}
    const action = request.action

    if (action === 'open') {
      const url = normalizeWebUrl(args.url)
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT })
      await this.settle(page)
      return this.result(page, session, `已打开 ${url}`)
    }
    if (action === 'snapshot') {
      const snapshot = await this.snapshot(page, Number(args.maxElements ?? 140))
      return {
        text: this.formatSnapshot(snapshot, session, page),
        url: snapshot.url,
        title: snapshot.title
      }
    }
    if (action === 'extract') {
      const maxChars = Math.max(500, Math.min(30_000, Number(args.maxChars ?? 12_000) || 12_000))
      const text =
        args.ref || args.selector || args.label || args.text
          ? await (await this.locatorFor(page, args)).innerText({ timeout: DEFAULT_TIMEOUT })
          : await page.locator('body').innerText({ timeout: DEFAULT_TIMEOUT })
      return this.result(
        page,
        session,
        text.length > maxChars
          ? `${text.slice(0, maxChars)}\n（已截断）`
          : text || '（页面没有可见文字）'
      )
    }
    if (action === 'screenshot') {
      const dir = join(this.tempRoot, 'screenshots')
      mkdirSync(dir, { recursive: true })
      const path = join(dir, `browser-${Date.now()}-${randomUUID().slice(0, 8)}.png`)
      await page.screenshot({ path, fullPage: args.fullPage === true })
      return this.result(page, session, `截图：${path}`, path)
    }
    if (action === 'back') {
      await page.goBack({ waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT })
      await this.settle(page)
      return this.result(page, session, '已返回上一页')
    }
    if (action === 'reload') {
      await page.reload({ waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT })
      await this.settle(page)
      return this.result(page, session, '已刷新页面')
    }
    if (action === 'pages') {
      const pages = [...session.pages].filter((item) => !item.isClosed())
      const lines = await Promise.all(
        pages.map(async (item, index) => {
          const title = await item.title().catch(() => '')
          return `${index + 1}. ${item === page ? '[当前] ' : ''}${title || '（无标题）'} ${item.url()}`
        })
      )
      return this.result(page, session, lines.join('\n'))
    }
    if (action === 'switch') {
      const index = Math.max(1, Math.floor(Number(args.index ?? 0) || 0)) - 1
      const pages = [...session.pages].filter((item) => !item.isClosed())
      const target = pages[index]
      if (!target) throw new Error(`没有第 ${index + 1} 个标签页`)
      session.page = target
      await this.attachPage(session, target)
      await target.bringToFront()
      return this.result(target, session, `已切换到标签页 ${index + 1}`)
    }
    if (action === 'close_tab') {
      const closing = session.page
      if (closing && !closing.isClosed()) await closing.close()
      if (closing) session.pages.delete(closing)
      session.page = null
      const next = [...session.pages].find((item) => !item.isClosed()) ?? (await context.newPage())
      session.page = next
      await this.attachPage(session, next)
      return this.result(next, session, closing ? '已关闭当前标签页' : '标签页已经关闭')
    }
    if (action === 'close') {
      // 子 Agent 共享同一用户浏览器 Context，但各自拥有独立标签页。
      // 单个任务的 close 只能结束自己的页面，不能把其他 Agent 的浏览器一起关掉。
      for (const item of session.pages) {
        if (!item.isClosed()) await item.close()
      }
      session.pages.clear()
      session.page = null
      this.sessions.delete(String(request.conversationId ?? '').trim() || 'default')
      return { text: '当前浏览器标签页已关闭；登录态仍保存在本机。', url: '', title: '' }
    }
    if (action === 'perform') {
      const steps = Array.isArray(args.steps) ? args.steps : []
      if (!steps.length) throw new Error('perform 需要 steps 数组')
      if (steps.length > MAX_STEPS) throw new Error(`一次最多执行 ${MAX_STEPS} 个步骤`)
      const done: string[] = []
      for (const [index, raw] of steps.entries()) {
        const step = (raw ?? {}) as Record<string, unknown>
        const name = String(step.action ?? '').trim()
        if (!name) throw new Error(`第 ${index + 1} 步缺少 action`)
        const text = await this.performStep(page, name, step)
        done.push(`${index + 1}. ${text}`)
      }
      return this.result(page, session, `批量执行完成：\n${done.join('\n')}`)
    }

    const done = await this.performStep(page, action, args)
    return this.result(page, session, done)
  }

  async run(request: BrowserOperationRequest): Promise<BrowserOperationResult> {
    if (!request || typeof request.action !== 'string') throw new Error('缺少浏览器动作')
    if (!this.root) throw new Error('浏览器自动化尚未初始化')
    this.assertAccepting()
    const { session, page } = await this.sessionFor(request.userId, request.conversationId)
    const context = this.context
    if (!context) throw new Error('浏览器上下文不可用')
    try {
      await this.assertSessionPagesSafe(session)
      const result = await this.execute(request, page, session, context)
      await this.assertSessionPagesSafe(session)
      return result
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      if (!page.isClosed() && safeWebPageUrl(page.url())) {
        const snapshot = await this.snapshot(page, 60).catch(() => null)
        if (snapshot) {
          throw new Error(
            `浏览器动作失败：${message}\n\n当前页面：\n${this.formatSnapshot(
              snapshot,
              session,
              page
            )}`
          )
        }
      }
      if (!page.isClosed() && !safeWebPageUrl(page.url())) {
        await page.close().catch(() => undefined)
      }
      throw error
    }
  }

  /** 停止生成时关闭该对话的标签页，并清掉后续动作可继续跑的页面。 */
  cancel(conversationId: unknown): void {
    const key = String(conversationId ?? '').trim() || 'default'
    const session = this.sessions.get(key)
    if (!session) return
    this.sessions.delete(key)
    for (const page of session.pages) {
      if (!page.isClosed()) void page.close().catch(() => undefined)
    }
    session.pages.clear()
    session.page = null
  }

  async closeAll(): Promise<void> {
    this.closeCount++
    try {
      // 等已经发出的 launch 与正在关闭的旧 Context 完成，再拿最终要关闭的上下文。
      // closeCount 让启动中的任务看到“正在关闭”，又能在注销后重新开放浏览器。
      await Promise.allSettled([...this.pendingLaunches, ...this.pendingContextClosures])
      const context = this.context
      this.context = null
      this.profileKey = ''
      this.sessions.clear()
      if (context) await context.close().catch(() => undefined)
    } finally {
      this.closeCount--
    }
  }

  /** 客户端退出专用：先禁止新的浏览器任务，再关闭所有已启动与启动中的浏览器。 */
  async shutdown(): Promise<void> {
    this.closing = true
    await this.closeAll()
  }
}

export const browserAutomation = new BrowserAutomation()
