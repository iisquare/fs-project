import { randomUUID } from 'crypto'
import { mkdirSync, readdirSync, statSync, unlinkSync, writeFileSync } from 'fs'
import { join } from 'path'
import {
  desktopCapturer,
  nativeImage,
  screen as electronScreen,
  shell,
  systemPreferences
} from 'electron'
import {
  Button,
  FileType,
  Key,
  Region,
  getActiveWindow,
  getWindows,
  keyboard,
  mouse,
  screen,
  sleep,
  type Point,
  type Window
} from '@nut-tree-fork/nut-js'
import { issueChatImage } from './images'
import { LocalizedError } from './logging'
import { readWindowsWindowTitles, usableWindowTitle } from './window-titles'
import type {
  DesktopAutomationSupportStatus,
  DesktopPermissionKey,
  DesktopPermissionState,
  DesktopPermissionStatus
} from '../shared/desktop'

export type DesktopAction =
  | 'windows'
  | 'snapshot'
  | 'focus'
  | 'click'
  | 'type_text'
  | 'press'
  | 'scroll'
  | 'drag'
  | 'wait'
  | 'screenshot'
  | 'close'

export interface DesktopOperationRequest {
  projectId?: string
  conversationId?: string
  sourceSessionId?: string
  action: DesktopAction
  args?: Record<string, unknown>
}

export interface DesktopOperationResult {
  text: string
  screenshot?: string
  /** 给模型看的压缩截图；只进当前调用链，不落库 */
  image?: string
  width?: number
  height?: number
}

export interface DesktopApprovalRequest {
  id: string
  kind: 'desktop'
  projectId: string
  path: ''
  need: 'desktop'
  action: DesktopAction
  title: string
  detail: string
  conversationId?: string
  sourceSessionId?: string
}

interface DesktopSession {
  windows: Window[]
  identities: WindowIdentity[]
  target: string
  targetIdentity: WindowIdentity | null
  updatedAt: number
}

interface WindowIdentity {
  handle: number | null
  title: string
  region: Region
}

const MAX_WINDOWS = 40
const MAX_TEXT = 10_000
const MAX_IMAGE_WIDTH = 1280
const SESSION_TTL = 20 * 60 * 1000
const TEMP_RETENTION = 7 * 24 * 60 * 60 * 1000

export function desktopCaptureErrorMessage(platform: NodeJS.Platform): {
  message: string
  log: string
} {
  if ('win32' === platform) {
    return {
      message: 'Windows 桌面截图失败，请确认屏幕已解锁且显示器连接正常后重试',
      log: 'Desktop screenshot failed on Windows; verify the screen is unlocked and a display is available'
    }
  }
  if ('darwin' === platform) {
    return {
      message: '桌面截图失败，请检查 macOS 屏幕录制权限',
      log: 'Desktop screenshot failed; check macOS screen-recording permission'
    }
  }
  return {
    message: '桌面截图失败，请确认显示器可用后重试',
    log: 'Desktop screenshot failed; verify a display is available'
  }
}

export function isDesktopPermissionFailure(
  error: unknown,
  platform: NodeJS.Platform = process.platform
): boolean {
  // 屏幕录制授权只由 macOS 通过 TCC 管理。Windows 的截图失败可能来自锁屏、
  // 显示器切换或受保护内容，不能据此弹出系统权限设置。
  if ('darwin' !== platform) return false
  const message = error instanceof Error ? error.message : String(error ?? '')
  return /(?:桌面.*权限|辅助功能|屏幕录制|screen recording|accessibility|permission|capture failed)/i.test(
    message
  )
}

function key(value: unknown): string {
  return String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
}

function clip(value: unknown, max = 160): string {
  const text = key(String(value ?? ''))
  return text.length > max ? `${text.slice(0, max)}...` : text
}

function windowHandle(window: Window): number | null {
  const handle = Number((window as unknown as { windowHandle?: unknown }).windowHandle)
  return Number.isSafeInteger(handle) && handle > 0 ? handle : null
}

function nativeWindowTitleFallback(title: string): string {
  return 'win32' === process.platform ? usableWindowTitle(title) : title
}

function sameWindow(
  left: { handle?: number | null; title: string; region: Region },
  right: { handle?: number | null; title: string; region: Region }
): boolean {
  if (null != left.handle && null != right.handle) return left.handle === right.handle
  return (
    left.title === right.title &&
    left.region.left === right.region.left &&
    left.region.top === right.region.top &&
    left.region.width === right.region.width &&
    left.region.height === right.region.height
  )
}

function rectangle(region: Region): Electron.Rectangle {
  return {
    x: region.left,
    y: region.top,
    width: region.width,
    height: region.height
  }
}

function pixel(region: Region, x: unknown, y: unknown): Point {
  const nx = Math.max(0, Math.min(1000, Number(x ?? 500) || 0))
  const ny = Math.max(0, Math.min(1000, Number(y ?? 500) || 0))
  return {
    x: Math.round(region.left + (region.width - 1) * (nx / 1000)),
    y: Math.round(region.top + (region.height - 1) * (ny / 1000))
  }
}

function modifier(name: string, platform: NodeJS.Platform): Key | null {
  const value = name.toLowerCase().replace(/[\s_-]+/g, '')
  if ('ctrl' === value || 'control' === value) return Key.LeftControl
  if (
    'win' === value ||
    'windows' === value ||
    'lwin' === value ||
    'leftwin' === value ||
    'leftmeta' === value
  ) {
    return platform === 'darwin' ? Key.LeftCmd : Key.LeftWin
  }
  if ('cmd' === value || 'command' === value || 'meta' === value || 'super' === value) {
    return platform === 'darwin' ? Key.LeftCmd : Key.LeftWin
  }
  if ('alt' === value || 'option' === value) return Key.LeftAlt
  if ('shift' === value) return Key.LeftShift
  return null
}

function namedKey(name: string): Key | null {
  const value = name.trim().toLowerCase()
  const fixed: Record<string, Key> = {
    enter: Key.Enter,
    return: Key.Return,
    tab: Key.Tab,
    escape: Key.Escape,
    esc: Key.Escape,
    backspace: Key.Backspace,
    delete: Key.Delete,
    insert: Key.Insert,
    home: Key.Home,
    end: Key.End,
    pageup: Key.PageUp,
    pagedown: Key.PageDown,
    up: Key.Up,
    down: Key.Down,
    left: Key.Left,
    right: Key.Right,
    space: Key.Space,
    f1: Key.F1,
    f2: Key.F2,
    f3: Key.F3,
    f4: Key.F4,
    f5: Key.F5,
    f6: Key.F6,
    f7: Key.F7,
    f8: Key.F8,
    f9: Key.F9,
    f10: Key.F10,
    f11: Key.F11,
    f12: Key.F12,
    '*': Key.Multiply,
    multiply: Key.Multiply,
    '/': Key.Slash,
    divide: Key.Divide,
    '-': Key.Minus,
    minus: Key.Minus,
    subtract: Key.Subtract,
    '+': Key.Add,
    add: Key.Add,
    '.': Key.Period,
    period: Key.Period,
    decimal: Key.Decimal,
    '=': Key.Equal,
    equal: Key.Equal,
    '\\': Key.Backslash,
    backslash: Key.Backslash,
    '[': Key.LeftBracket,
    leftbracket: Key.LeftBracket,
    ']': Key.RightBracket,
    rightbracket: Key.RightBracket,
    ';': Key.Semicolon,
    semicolon: Key.Semicolon,
    "'": Key.Quote,
    quote: Key.Quote,
    ',': Key.Comma,
    comma: Key.Comma,
    '`': Key.Grave,
    grave: Key.Grave
  }
  // Escape and F1 are valid enum values beginning at zero, so truthiness rejects them.
  if (fixed[value] !== undefined) return fixed[value]
  if (/^[a-z]$/.test(value)) return (Key as unknown as Record<string, Key>)[value.toUpperCase()]
  if (/^[0-9]$/.test(value)) return (Key as unknown as Record<string, Key>)[`Num${value}`]
  return null
}

export function keyCombination(
  value: unknown,
  platform: NodeJS.Platform = process.platform
): Key[] {
  const source = String(value ?? '').trim()
  const parts = ('+' === source ? [source] : source.split('+'))
    .map((item) => item.trim())
    .filter(Boolean)
  if (!parts.length) {
    throw new LocalizedError(
      'DESKTOP_KEY_REQUIRED',
      'press 需要 key，例如 Enter、Ctrl+A、Cmd+Space',
      'press requires a key, for example Enter, Ctrl+A or Cmd+Space'
    )
  }
  const keys = parts.map((part) => modifier(part, platform) ?? namedKey(part))
  if (keys.some((item) => item === null)) {
    throw new LocalizedError(
      'DESKTOP_KEY_UNSUPPORTED',
      `无法识别按键：${parts.join('+')}`,
      `Unsupported key combination: ${parts.join('+')}`
    )
  }
  return keys as Key[]
}

function screenRegion(): Region {
  const display = electronScreen.getDisplayNearestPoint(electronScreen.getCursorScreenPoint())
  return new Region(display.bounds.x, display.bounds.y, display.bounds.width, display.bounds.height)
}

/**
 * 跨 Windows / macOS 的桌面执行器。
 *
 * 第一版以视觉坐标操作为主：每次观察返回一张压缩截图，模型使用 0-1000 的归一化坐标，
 * 执行器再映射到窗口的实际区域，避免截图缩放、Retina 与普通 DPI 造成的坐标误差。
 * 窗口枚举、鼠标键盘和截图由 nut.js 的本地原生模块完成，不开放网络监听端口。
 */
export class DesktopAutomation {
  private root = ''
  private sessions = new Map<string, DesktopSession>()
  /** 只记录执行器真正按下、尚未释放的按键，避免退出时凭空发送按键抬起事件。 */
  private heldButtons = new Set<Button>()
  private stopped = false
  /** 当前进程里配置过屏幕录制权限；macOS 只把该授权应用到新启动的进程。 */
  private screenRecordingRestartRequired = false

  init(runtimeDir: string): void {
    this.root = join(runtimeDir, 'userdata', 'automation', 'desktop')
    mkdirSync(this.tempRoot, { recursive: true })
    this.pruneTemp()
    mouse.config.autoDelayMs = 12
    keyboard.config.autoDelayMs = 12
  }

  private get tempRoot(): string {
    return join(this.root, 'temp')
  }

  /** 截图是会话历史的一部分，保留最近 7 天；更早的临时文件在启动时清掉。 */
  private pruneTemp(): void {
    const cutoff = Date.now() - TEMP_RETENTION
    let names: string[]
    try {
      names = readdirSync(this.tempRoot)
    } catch {
      return
    }
    for (const name of names) {
      const path = join(this.tempRoot, name)
      try {
        if (statSync(path).mtimeMs >= cutoff) continue
        unlinkSync(path)
      } catch {
        // 另一个进程或清理任务可能已经删掉它，继续处理其余文件。
      }
    }
  }

  private async releaseHeldButtons(): Promise<void> {
    const buttons = [...this.heldButtons]
    this.heldButtons.clear()
    if (!buttons.length) return
    await Promise.allSettled(buttons.map((button) => mouse.releaseButton(button)))
  }

  status(): DesktopAutomationSupportStatus {
    const supported = 'win32' === process.platform || 'darwin' === process.platform
    if (!supported) {
      return {
        supported: false,
        permissionsChecked: false,
        permissionsReady: false,
        restartRequired: false,
        permissions: [],
        platform: process.platform,
        reason: '当前版本仅支持 Windows 与 macOS'
      }
    }
    if ('win32' === process.platform) {
      return {
        supported: true,
        permissionsChecked: false,
        permissionsReady: true,
        restartRequired: false,
        permissions: [],
        platform: process.platform,
        reason: ''
      }
    }
    // macOS 权限只在用户尝试开启桌面操作时读取，启动和普通页面加载不触发系统授权检查。
    return {
      supported: true,
      permissionsChecked: false,
      permissionsReady: false,
      restartRequired: false,
      permissions: [],
      platform: process.platform,
      reason: ''
    }
  }

  /**
   * 启用开关时的只读检查。
   *
   * 不主动调用系统授权弹窗或截图请求：macOS 在配置屏幕录制后会要求进程重启，
   * 由应用主动触发会把“打开开关”变成一次系统级 Quit & Reopen。这里只判断状态，
   * 缺权限时仍保存开关并返回检测结果，由界面弹出授权面板。
   */
  checkPermissionsForEnable(): DesktopAutomationSupportStatus {
    if ('darwin' !== process.platform) return this.status()
    const current = this.permissionStatus()
    const screenRecording = current.permissions.find((item) => 'screenRecording' === item.key)
    if (screenRecording && !screenRecording.granted) {
      this.screenRecordingRestartRequired = true
      return {
        ...current,
        permissionsReady: false,
        restartRequired: true,
        reason: `${current.reason}。授予屏幕录制权限后，请完全退出并重新打开 fs-bot，再开启桌面操作`
      }
    }
    if (this.screenRecordingRestartRequired) {
      return {
        ...current,
        permissionsReady: false,
        restartRequired: true,
        reason: '屏幕录制权限已配置，但需要手动重启 fs-bot 后才对当前进程生效'
      }
    }
    return current
  }

  /** 桌面操作执行前复核当前进程的权限，不触发系统授权弹窗。 */
  checkPermissionsForRun(): DesktopAutomationSupportStatus {
    if ('darwin' !== process.platform) return this.status()
    const current = this.permissionStatus()
    const screenRecording = current.permissions.find((item) => 'screenRecording' === item.key)
    if (screenRecording && !screenRecording.granted) {
      this.screenRecordingRestartRequired = true
      return {
        ...current,
        permissionsReady: false,
        restartRequired: true,
        reason: `${current.reason}。授予屏幕录制权限后，请完全退出并重新打开 fs-bot 再执行桌面操作`
      }
    }
    if (this.screenRecordingRestartRequired) {
      return {
        ...current,
        permissionsReady: false,
        restartRequired: true,
        reason: '屏幕录制权限已变更，请完全退出并重新打开 fs-bot 后再执行桌面操作'
      }
    }
    return current
  }

  private permissionStatus(): DesktopAutomationSupportStatus {
    const supported = 'win32' === process.platform || 'darwin' === process.platform
    if (!supported) return this.status()
    if ('win32' === process.platform) {
      return {
        supported: true,
        permissionsChecked: true,
        permissionsReady: true,
        restartRequired: false,
        permissions: [],
        platform: process.platform,
        reason: ''
      }
    }
    let accessibilityGranted = false
    let screenState: DesktopPermissionState = 'unknown'
    try {
      accessibilityGranted = systemPreferences.isTrustedAccessibilityClient(false)
    } catch {
      // 权限查询失败时按未授权处理，本次启用失败。
    }
    try {
      screenState = systemPreferences.getMediaAccessStatus('screen')
    } catch {
      // 状态无法读取时不假定已授权。
    }
    const permissions: DesktopPermissionStatus[] = [
      {
        key: 'accessibility',
        name: '辅助功能',
        description: '用于查询窗口并执行鼠标、键盘操作',
        // 系统 API 只能回答“是否已信任”，无法区分从未申请与已经拒绝。
        state: accessibilityGranted ? 'granted' : 'unknown',
        granted: accessibilityGranted
      },
      {
        key: 'screenRecording',
        name: '屏幕录制',
        description: '用于读取当前桌面和窗口画面',
        state: screenState,
        granted: 'granted' === screenState
      }
    ]
    const missing = permissions.filter((item) => !item.granted).map((item) => item.name)
    return {
      supported: true,
      permissionsChecked: true,
      permissionsReady: missing.length === 0,
      restartRequired: false,
      permissions,
      platform: process.platform,
      reason: missing.length
        ? `缺少 macOS 系统权限：${missing.join('、')}。请完成配置后再使用桌面操作`
        : ''
    }
  }

  async openPermissionSettings(permission: DesktopPermissionKey): Promise<void> {
    if ('darwin' !== process.platform) return
    const pane =
      'accessibility' === permission
        ? 'Privacy_Accessibility'
        : 'screenRecording' === permission
          ? 'Privacy_ScreenCapture'
          : ''
    if (!pane) {
      throw new LocalizedError(
        'DESKTOP_PERMISSION_UNKNOWN',
        '无法识别的系统权限',
        'Unknown desktop permission'
      )
    }
    await shell.openExternal(`x-apple.systempreferences:com.apple.preference.security?${pane}`)
  }

  private session(request: DesktopOperationRequest): DesktopSession {
    const id =
      key(request.sourceSessionId) || key(request.conversationId) || `desktop-${randomUUID()}`
    let current = this.sessions.get(id)
    if (!current || Date.now() - current.updatedAt > SESSION_TTL) {
      current = {
        windows: [],
        identities: [],
        target: '',
        targetIdentity: null,
        updatedAt: Date.now()
      }
      this.sessions.set(id, current)
    }
    current.updatedAt = Date.now()
    return current
  }

  private identityFor(session: DesktopSession, target: unknown): WindowIdentity | null {
    const ref = key(target)
    const index = Number(ref.replace(/^w/i, '')) - 1
    return Number.isInteger(index) && index >= 0 ? (session.identities[index] ?? null) : null
  }

  private async refreshWindows(
    session: DesktopSession,
    requestedTarget?: unknown
  ): Promise<string> {
    const requested = key(requestedTarget)
    const desired =
      'screen' === requested
        ? null
        : requested
          ? (this.identityFor(session, requested) ?? session.targetIdentity)
          : null
    const all = await getWindows()
    const active = await getActiveWindow()
    const activeTitle = await active.title
    const activeRegion = await active.region
    const activeHandle = windowHandle(active)
    const rows: Array<{
      window: Window
      handle: number | null
      title: string
      region: Region
      active: boolean
    }> = []
    const handles: number[] = []
    for (const window of all) {
      try {
        const handle = windowHandle(window)
        const title = key(await window.title)
        const region = await window.region
        if ('__fs_bot_desktop_overlay__' === title) continue
        if (!title || region.width < 40 || region.height < 30) continue
        rows.push({
          window,
          handle,
          title,
          region,
          active: sameWindow(
            { handle, title, region },
            {
              handle: activeHandle,
              title: key(activeTitle),
              region: activeRegion
            }
          )
        })
        if (handle !== null) handles.push(handle)
      } catch {
        // 窗口可能在枚举期间关闭，跳过即可。
      }
    }
    let unicodeTitles = new Map<number, string>()
    try {
      unicodeTitles = await readWindowsWindowTitles(handles)
    } catch (error) {
      console.warn(
        '[DesktopAutomation] Unicode window titles unavailable, using safe fallback:',
        error
      )
    }
    for (const row of rows) {
      const unicodeTitle = row.handle === null ? undefined : unicodeTitles.get(row.handle)
      row.title = unicodeTitle ?? nativeWindowTitleFallback(row.title)
    }
    rows.sort((left, right) => Number(right.active) - Number(left.active))
    const visible = rows.slice(0, MAX_WINDOWS)
    session.windows = visible.map((item) => item.window)
    session.identities = visible.map((item) => ({
      handle: item.handle,
      title: item.title,
      region: item.region
    }))
    if ('screen' === requested) {
      session.target = 'screen'
      session.targetIdentity = null
    } else {
      const matched = desired ? visible.findIndex((item) => sameWindow(item, desired)) : -1
      const index = matched >= 0 ? matched : visible.length ? 0 : -1
      session.target = index >= 0 ? `w${index + 1}` : 'screen'
      session.targetIdentity = index >= 0 ? session.identities[index] : null
    }
    const lines = ['可用窗口（用 snapshot 获取当前画面）：']
    if (!rows.length) lines.push('（没有发现可见窗口）')
    rows.slice(0, MAX_WINDOWS).forEach((row, index) => {
      const display = electronScreen.getDisplayMatching(rectangle(row.region))
      lines.push(
        `w${index + 1} ${row.active ? '[当前] ' : ''}${clip(row.title)}` +
          ` (${row.region.width}x${row.region.height}, ${clip(display.label || `显示器 ${display.id}`, 40)})`
      )
    })
    return lines.join('\n')
  }

  private async targetWindow(session: DesktopSession, target: unknown): Promise<Window> {
    const ref = key(String(target ?? session.target))
    if ('screen' === ref) {
      throw new LocalizedError(
        'DESKTOP_TARGET_NOT_WINDOW',
        '当前目标不是具体窗口，请先 snapshot 并选择窗口',
        'The desktop target is not a concrete window; call snapshot and select a window'
      )
    }
    const index = Number(ref.replace(/^w/i, '')) - 1
    const window = session.windows[index]
    if (!window) {
      throw new LocalizedError(
        'DESKTOP_TARGET_STALE',
        '窗口引用已失效，请重新调用 snapshot',
        'The window reference is stale; call snapshot again'
      )
    }
    const region = await window.region
    if (region.width < 1 || region.height < 1) {
      throw new LocalizedError(
        'DESKTOP_TARGET_HIDDEN',
        '窗口当前不可见或已最小化，请先 focus 或恢复窗口',
        'The target window is hidden or minimized; focus or restore it first'
      )
    }
    session.target = ref
    const known = session.identities[index]
    session.targetIdentity = {
      handle: windowHandle(window),
      title: known?.title ?? key(await window.title),
      region
    }
    return window
  }

  private async capture(
    session: DesktopSession,
    target: unknown
  ): Promise<{ url: string; image: string; width: number; height: number; label: string }> {
    let region: Region
    let display: Electron.Display
    let label = ''
    const ref = key(String(target ?? session.target))
    if ('screen' === ref) {
      display = electronScreen.getDisplayNearestPoint(electronScreen.getCursorScreenPoint())
      region = screenRegion()
      label = display.label || `显示器 ${display.id}`
      session.target = 'screen'
    } else {
      const window = await this.targetWindow(session, ref)
      await window.focus()
      await sleep(120)
      region = await window.region
      display = electronScreen.getDisplayMatching(rectangle(region))
      label = session.targetIdentity?.title ?? nativeWindowTitleFallback(key(await window.title))
    }
    const base = `desktop-${Date.now()}-${randomUUID().slice(0, 8)}`
    const original = await this.captureRegion(region, display)
    if (original.isEmpty()) {
      const failure = desktopCaptureErrorMessage(process.platform)
      throw new LocalizedError('DESKTOP_CAPTURE_FAILED', failure.message, failure.log)
    }
    const size = original.getSize()
    const resized =
      size.width > MAX_IMAGE_WIDTH ? original.resize({ width: MAX_IMAGE_WIDTH }) : original
    const nextSize = resized.getSize()
    const jpeg = resized.toJPEG(72)
    if (!jpeg.length) {
      throw new LocalizedError(
        'DESKTOP_CAPTURE_ENCODE_FAILED',
        '桌面截图编码失败',
        'Desktop screenshot encoding failed'
      )
    }
    const path = join(this.tempRoot, `${base}.jpg`)
    writeFileSync(path, jpeg)
    return {
      url: issueChatImage(path, 'image/jpeg'),
      image: `data:image/jpeg;base64,${jpeg.toString('base64')}`,
      width: nextSize.width,
      height: nextSize.height,
      label
    }
  }

  /**
   * 从窗口所在显示器捕获整屏后裁出窗口。
   *
   * nut.js 的 Region 截图接口只覆盖主显示器；Electron desktopCapturer 能拿到每块屏幕，
   * 因此多屏环境下先按窗口中心选中显示器，再以 DIP 比例换算裁剪区域。
   */
  private async captureRegion(
    region: Region,
    display: Electron.Display
  ): Promise<Electron.NativeImage> {
    const width = Math.max(1, Math.round(display.size.width * display.scaleFactor))
    const height = Math.max(1, Math.round(display.size.height * display.scaleFactor))
    try {
      const sources = await desktopCapturer.getSources({
        types: ['screen'],
        thumbnailSize: { width, height },
        fetchWindowIcons: false
      })
      const displayIndex = electronScreen
        .getAllDisplays()
        .findIndex((item) => item.id === display.id)
      const source =
        sources.find((item) => String(item.display_id) === String(display.id)) ??
        (displayIndex >= 0 ? sources[displayIndex] : undefined) ??
        sources[0]
      if (source && !source.thumbnail.isEmpty()) {
        const full = source.thumbnail
        const fullSize = full.getSize()
        const bounds = display.bounds
        const left = Math.max(bounds.x, region.left)
        const top = Math.max(bounds.y, region.top)
        const right = Math.min(bounds.x + bounds.width, region.left + region.width)
        const bottom = Math.min(bounds.y + bounds.height, region.top + region.height)
        if (right > left && bottom > top) {
          const scaleX = fullSize.width / bounds.width
          const scaleY = fullSize.height / bounds.height
          return full.crop({
            x: Math.max(0, Math.round((left - bounds.x) * scaleX)),
            y: Math.max(0, Math.round((top - bounds.y) * scaleY)),
            width: Math.max(1, Math.round((right - left) * scaleX)),
            height: Math.max(1, Math.round((bottom - top) * scaleY))
          })
        }
      }
    } catch (error) {
      console.warn('[DesktopAutomation] desktopCapturer failed, using primary display:', error)
    }

    // 仅当目标就在主屏（或 desktopCapturer 不可用）时回退到 nut.js。
    const primary = electronScreen.getPrimaryDisplay().bounds
    if (
      region.left >= primary.x &&
      region.top >= primary.y &&
      region.left + region.width <= primary.x + primary.width &&
      region.top + region.height <= primary.y + primary.height
    ) {
      const raw = await screen.captureRegion(
        `desktop-fallback-${Date.now()}-${randomUUID().slice(0, 8)}`,
        region,
        FileType.PNG,
        this.tempRoot,
        '',
        ''
      )
      const image = nativeImage.createFromPath(raw)
      try {
        unlinkSync(raw)
      } catch {
        // 原始 PNG 只是中间产物，删除失败不会影响本次操作。
      }
      return image
    }
    return nativeImage.createEmpty()
  }

  private async snapshot(
    session: DesktopSession,
    target: unknown
  ): Promise<DesktopOperationResult> {
    const windows = await this.refreshWindows(session, target)
    const shot = await this.capture(session, session.target)
    return {
      text: [
        windows,
        '',
        `当前截图：${clip(shot.label)}（${shot.width}x${shot.height}）`,
        '坐标口径：x / y 都是相对当前截图左上角的 0-1000 归一化坐标；只做下一步，动作完成后再看图核实。',
        `![桌面截图](${shot.url})`
      ].join('\n'),
      screenshot: shot.url,
      image: shot.image,
      width: shot.width,
      height: shot.height
    }
  }

  async run(request: DesktopOperationRequest): Promise<DesktopOperationResult> {
    const status = this.status()
    if (!status.supported) {
      throw new LocalizedError(
        'DESKTOP_UNSUPPORTED',
        status.reason,
        `Desktop automation is not supported on ${process.platform}`
      )
    }
    if (this.stopped) {
      throw new LocalizedError(
        'DESKTOP_STOPPED',
        '桌面操作已被紧急停止；重新授权后才能继续',
        'Desktop automation was stopped; authorize it again before continuing'
      )
    }
    const session = this.session(request)
    const args = request.args ?? {}
    const action = request.action
    if ('windows' === action) return { text: await this.refreshWindows(session) }
    if ('snapshot' === action || 'screenshot' === action) {
      return this.snapshot(session, args.target)
    }
    if ('focus' === action) {
      const window = await this.targetWindow(session, args.target)
      await window.focus()
      await sleep(180)
      session.target = key(args.target)
      return this.snapshot(session, undefined)
    }
    if ('click' === action) {
      const target = key(args.target ?? session.target)
      let region: Region
      if ('screen' === target) {
        session.target = 'screen'
        region = screenRegion()
      } else {
        const window = await this.targetWindow(session, args.target)
        await window.focus()
        await sleep(100)
        region = await window.region
      }
      const point = pixel(region, args.x, args.y)
      await mouse.setPosition(point)
      const right = 'right' === key(args.button).toLowerCase()
      if (args.double === true) await mouse.doubleClick(right ? Button.RIGHT : Button.LEFT)
      else await mouse.click(right ? Button.RIGHT : Button.LEFT)
      await sleep(220)
      return this.snapshot(session, undefined)
    }
    if ('type_text' === action) {
      const text = String(args.text ?? '')
      if (!text) {
        throw new LocalizedError(
          'DESKTOP_TEXT_REQUIRED',
          'type_text 需要 text',
          'type_text requires text'
        )
      }
      if (text.length > MAX_TEXT) {
        throw new LocalizedError(
          'DESKTOP_TEXT_TOO_LONG',
          `一次最多输入 ${MAX_TEXT} 个字符`,
          `At most ${MAX_TEXT} characters can be entered at once`
        )
      }
      await keyboard.type(text)
      await sleep(150)
      return this.snapshot(session, undefined)
    }
    if ('press' === action) {
      const keys = keyCombination(args.key)
      await keyboard.pressKey(...keys)
      await keyboard.releaseKey(...keys)
      await sleep(180)
      return this.snapshot(session, undefined)
    }
    if ('scroll' === action) {
      const direction = key(args.direction ?? 'down').toLowerCase()
      const amount = Math.max(1, Math.min(30, Math.floor(Number(args.amount ?? 3) || 3)))
      if (undefined !== args.x || undefined !== args.y) {
        const target = key(args.target ?? session.target)
        let region: Region
        if ('screen' === target) {
          session.target = 'screen'
          region = screenRegion()
        } else {
          const window = await this.targetWindow(session, args.target)
          await window.focus()
          region = await window.region
        }
        await mouse.setPosition(pixel(region, args.x, args.y))
      }
      if ('up' === direction) await mouse.scrollUp(amount)
      else if ('left' === direction) await mouse.scrollLeft(amount)
      else if ('right' === direction) await mouse.scrollRight(amount)
      else await mouse.scrollDown(amount)
      await sleep(180)
      return this.snapshot(session, undefined)
    }
    if ('drag' === action) {
      const target = key(args.target ?? session.target)
      let region: Region
      if ('screen' === target) {
        session.target = 'screen'
        region = screenRegion()
      } else {
        const window = await this.targetWindow(session, args.target)
        await window.focus()
        region = await window.region
      }
      const from = pixel(region, args.fromX, args.fromY)
      const to = pixel(region, args.toX, args.toY)
      await mouse.pressButton(Button.LEFT)
      this.heldButtons.add(Button.LEFT)
      try {
        await mouse.move([from, to])
      } finally {
        // 正常完成和拖拽中断都由这里收口；如果紧急停止已经先释放，则不重复发送。
        if (this.heldButtons.delete(Button.LEFT)) {
          await mouse.releaseButton(Button.LEFT).catch(() => undefined)
        }
      }
      await sleep(220)
      return this.snapshot(session, undefined)
    }
    if ('wait' === action) {
      const ms = Math.max(0, Math.min(10_000, Math.floor(Number(args.ms ?? 800) || 800)))
      await sleep(ms)
      return this.snapshot(session, undefined)
    }
    if ('close' === action) {
      await this.releaseHeldButtons()
      session.windows = []
      session.target = ''
      return { text: '本次桌面会话已结束，没有关闭任何用户应用。' }
    }
    throw new LocalizedError(
      'DESKTOP_ACTION_UNSUPPORTED',
      `不支持的桌面操作：${String(action)}`,
      `Unsupported desktop action: ${String(action)}`
    )
  }

  emergencyStop(): void {
    this.stopped = true
    void this.releaseHeldButtons()
  }

  clearEmergency(): void {
    this.stopped = false
  }

  cancel(sessionId: string): void {
    const id = key(sessionId)
    const hadSession = id ? this.sessions.delete(id) : this.sessions.size > 0
    if (!id) this.sessions.clear()
    // 没有桌面会话时不要进入 nut.js 原生层：普通对话停止也会走到这里，
    // 无意义地释放鼠标在部分 macOS 原生模块上会造成整个主进程退出。
    if (!hadSession) return
    void this.releaseHeldButtons()
  }
}

export const desktopAutomation = new DesktopAutomation()
