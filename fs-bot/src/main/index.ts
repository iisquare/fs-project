import {
  app,
  shell,
  BrowserWindow,
  clipboard,
  dialog,
  globalShortcut,
  ipcMain,
  nativeImage,
  Notification,
  powerMonitor,
  protocol,
  screen,
  type NativeImage,
  type IpcMainInvokeEvent
} from 'electron'
import { join } from 'path'
import { mkdirSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import { release as osRelease } from 'os'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
import { DatabaseManager } from './database/manager'
import { UserSchemaTooNewError } from './database/migrations'
import { ConfigStore } from './config'
import { normalizeDesktopApprovalPolicy } from './desktop-policy'
import { SkillManager } from './skills'
import { exportProject, importProject } from './projects'
import { ICON_SCHEME, initIcons, pickIcon, serveIcon } from './icons'
import { CHAT_IMAGE_SCHEME, serveChatImage } from './images'
import { parseProjectEnvVars } from './project-env'
import { ScheduleManager } from './schedule'
import { getAutoStartStatus, setAutoStart } from './autostart'
import { browserAutomation, type BrowserOperationRequest } from './automation'
import {
  desktopAutomation,
  isDesktopPermissionFailure,
  type DesktopAction,
  type DesktopApprovalRequest,
  type DesktopOperationRequest
} from './desktop-automation'
import {
  DESKTOP_EMERGENCY_SHORTCUT,
  type DesktopAgentUnavailableNotice,
  type DesktopAutomationStatus,
  type DesktopAutomationSupportStatus,
  type DesktopPermissionKey
} from '../shared/desktop'
import { desktopOverlay } from './desktop-overlay'
import {
  cancelUpdateDownload,
  downloadAndInstallUpdate,
  downloadControlStatus,
  initUpdateState,
  pauseUpdateDownload,
  reportUpdateHealthy,
  updateState,
  type UpdateInstallRequest
} from './update'
import {
  WorkspaceManager,
  type WorkspacePermission,
  type WorkspaceRoot,
  type WorkspaceApproval
} from './workspace'
import type {
  DbInitUserRequest,
  DbSelectRequest,
  DbInsertRequest,
  DbUpdateRequest,
  DbUpsertRequest,
  DbSaveMessagesRequest,
  DbDeleteRequest,
  DbDeleteBatchRequest,
  DbSearchRequest,
  ConfigUpdateSettingRequest,
  ConfigLoginHistoryRequest,
  WorkspaceReadRequest,
  WorkspaceOpenRequest,
  WorkspaceWriteRequest,
  WorkspaceRenameRequest,
  WorkspaceProjectRequest,
  WorkspaceSearchRequest,
  WorkspaceReplaceRequest,
  WorkspaceCommandRequest,
  WorkspaceUploadRequest,
  WorkspacePolicyRequest,
  WorkspaceApprovalResponse,
  SkillInstallRequest,
  SkillFileRequest,
  ProjectExportRequest,
  WorkspaceDragRequest
} from './database/types'
import {
  CLIENT_UPGRADE_REQUIRED_CHANNEL,
  isClientUpgradeRequired,
  type ClientMeta
} from '../shared/client'
import { installAsciiConsoleLogging, LocalizedError } from './logging'
import { externalUrl } from './external-url'

// Electron prints rejected IPC handlers through console.error. Install the encoding guard before
// any handler can fail, so localized error messages never reach a GBK output reader as mojibake.
installAsciiConsoleLogging()

const darwinKernelMajor = Number(osRelease().split('.')[0] || 0)
const legacyMacGpu =
  'darwin' === process.platform && darwinKernelMajor > 0 && darwinKernelMajor < 21
if (legacyMacGpu) {
  // macOS 11 的 ANGLE/Metal 组合在截图和透明浮层路径上可能触发 EGL 异常并结束主进程。
  app.disableHardwareAcceleration()
}

process.on('uncaughtException', (error) => {
  console.error('[Main] Uncaught exception (application kept alive):', error)
})
process.on('unhandledRejection', (reason) => {
  console.error('[Main] Unhandled rejection (application kept alive):', reason)
})

/** projects.workspace 列存的是 JSON 文本：解析成目录清单，结构不对就当作没有（一律拒绝） */
function parseWorkspaceRoots(raw: unknown): WorkspaceRoot[] {
  if (typeof raw !== 'string' || !raw) return []
  try {
    const list = JSON.parse(raw)
    if (!Array.isArray(list)) return []
    return list.map((item) => ({
      path: String(item?.path ?? ''),
      permission: String(item?.permission ?? '') as WorkspacePermission,
      // 主目录：标了 primary 的那一行，命令默认在它里面跑；都没标时按清单顺序取第一个
      primary: true === item?.primary
    }))
  } catch {
    return []
  }
}

function openSafeExternalUrl(raw: string): void {
  const url = externalUrl(raw)
  if (!url) {
    console.warn('[Security] Blocked unsupported external URL')
    return
  }
  void shell.openExternal(url)
}

let mainWindow: BrowserWindow | null = null
const agentWindows = new Map<string, BrowserWindow>()

const LOGIN_WINDOW_SIZE = { width: 450, height: 600 }
const MAIN_WINDOW_SIZE = { width: 900, height: 670 }
/** 与 Element Plus 页面底色一致：窗口先完成原生缩放时，用同色填充尚未提交的渲染区域 */
const WINDOW_BACKGROUND_COLOR = '#f2f3f5'
let mainWindowMode: 'login' | 'main' = 'login'
const agentSnapshots = new Map<string, unknown>()
let quitHandshakeStarted = false
let quitHandshakeDone = false
let quitHandshakeTimer: NodeJS.Timeout | null = null
let quitCleanup: Promise<void> | null = null

/** 退出清理不能无限等待；超时后也必须结束主进程，不能把残留留给用户手动处理。 */
const QUIT_CLEANUP_TIMEOUT = 5_000

function waitAtMost(task: Promise<unknown>, timeout: number): Promise<void> {
  return new Promise<void>((resolve) => {
    let done = false
    const finish = (): void => {
      if (done) return
      done = true
      clearTimeout(timer)
      resolve()
    }
    const timer = setTimeout(finish, timeout)
    void task.then(finish, finish)
  })
}

/**
 * 主进程退出前只执行一次的资源清理。
 *
 * 浏览器自动化启动的是独立的浏览器进程，数据库还有 WAL / 完整性哈希需要落盘。
 * 这些收尾必须等待完成；同时设置总超时，任何一步失联都不能让客户端留在后台。
 */
function cleanupMainProcessResources(): Promise<void> {
  if (quitCleanup) return quitCleanup
  quitCleanup = waitAtMost(
    (async () => {
      schedule.stop()
      cancelUpdateDownload()
      desktopAutomation.emergencyStop()
      desktopAutomation.cancel('')
      desktopOverlay.destroy()
      globalShortcut.unregisterAll()

      // 检查点已经写完，后续不再接受任何界面操作。先撤掉所有窗口，
      // 避免清理期间又从 IPC 拉起命令、浏览器会话或新的访问申请。
      for (const window of BrowserWindow.getAllWindows()) {
        if (!window.isDestroyed()) window.destroy()
      }

      // 退出时所有待审批项都按拒绝收口，先把等待它们的业务调用释放掉
      for (const [id, pending] of [...approvals]) {
        approvals.delete(id)
        pending.settle(false)
      }
      desktopLeases.clear()
      desktopDenials.clear()

      // 两棵进程树互不依赖，并行收掉，避免一条清理链卡住另一条
      await Promise.allSettled([workspace.shutdown(), browserAutomation.shutdown()])
      db.close()
    })(),
    QUIT_CLEANUP_TIMEOUT
  ).finally(() => {
    // 超时也要尽量把本地库完整关闭；close() 本身是同步且幂等的
    try {
      db.close()
    } catch {
      // 数据库已经关闭或进程即将退出，不能再阻止退出
    }
  })
  return quitCleanup
}

/**
 * 正常关闭窗口 / 退出应用前，先让渲染进程把正在生成的回复写成最终检查点。
 * 渲染进程未响应时超时放行；崩溃无法握手，但每个模型轮次与工具结果本身已有增量检查点。
 */
function finishQuitHandshake(): void {
  // quitCleanup 同时承担“清理已开始”的闸门：定时器与渲染进程确认可能几乎同时到达，
  // 后续任何关闭事件都必须等这一个 Promise，不能提前把主进程放走。
  if (quitCleanup) return
  if (quitHandshakeTimer) {
    clearTimeout(quitHandshakeTimer)
    quitHandshakeTimer = null
  }
  void cleanupMainProcessResources().finally(() => {
    // 只有资源真正收尾后才允许 before-quit / 窗口 close 继续，避免第二次关闭事件
    // 在浏览器或命令子进程尚未退出时提前结束主进程。
    quitHandshakeDone = true
    // app.quit() 仍可能被某个窗口的关闭钩子拖住；这里直接结束主进程，
    // Electron 随主进程退出时，其渲染 / GPU 子进程也会被回收。
    app.exit(0)
  })
}

function requestQuitHandshake(): void {
  if (quitHandshakeDone || quitHandshakeStarted) return
  quitHandshakeStarted = true
  if (!mainWindow || mainWindow.isDestroyed() || mainWindow.webContents.isDestroyed()) {
    finishQuitHandshake()
    return
  }
  mainWindow.webContents.send('app:prepare-quit')
  quitHandshakeTimer = setTimeout(finishQuitHandshake, 3_000)
}

/**
 * 用户库、子 Agent 会话锁与浏览器 Profile 都以当前用户为作用域，不支持两个进程同时写。
 * 第二次启动只激活已有窗口，避免同一 task_key / session_id 在两个渲染进程里并发执行。
 */
const singleInstance = app.requestSingleInstanceLock()
if (!singleInstance) {
  app.quit()
} else {
  app.on('second-instance', () => {
    if (!mainWindow || mainWindow.isDestroyed()) return
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show()
    mainWindow.focus()
  })
}

const db = DatabaseManager.getInstance()
const workspace = WorkspaceManager.getInstance()
const skills = SkillManager.getInstance()
const schedule = new ScheduleManager()

/** 所有客户端业务请求共用这份身份，避免渲染进程或某条下载链路漏掉字段。 */
function currentClientMeta(): ClientMeta {
  const settings = ConfigStore.getInstance().getSettings()
  return {
    clientVersion: app.getVersion(),
    platform: process.platform,
    architecture: process.arch,
    clientId: String(settings.clientId ?? ''),
    channel: String(settings.updateChannel ?? 'stable')
  }
}

/**
 * 主进程直发的客户端请求遇到版本闸门时，除了把错误交回调用方，
 * 还主动通知主窗口进入统一升级流程。普通网络错误不会被误判成升级。
 */
async function withClientUpgradeNotification<T>(
  event: IpcMainInvokeEvent,
  task: () => Promise<T> | T
): Promise<T> {
  try {
    return await task()
  } catch (error) {
    if (isClientUpgradeRequired(error) && !event.sender.isDestroyed()) {
      event.sender.send(CLIENT_UPGRADE_REQUIRED_CHANNEL)
    }
    throw error
  }
}

/**
 * 等待界面答复的访问申请：id → 答复回调与它所属的会话。
 * 成对清理：界面答复、等到超时、这段会话被停止，三条路都会收尾。
 */
const approvals = new Map<
  string,
  {
    kind: 'workspace' | 'desktop'
    conversationId: string
    sourceSessionId: string
    leaseKey: string
    settle: (allow: boolean) => void
  }
>()
const APPROVAL_TIMEOUT = 5 * 60 * 1000
const DESKTOP_LEASE_TTL = 20 * 60 * 1000
const desktopLeases = new Map<string, number>()
/** 已拒绝或终止的桌面任务：后续工具调用直接返回，不再重复弹授权。有效期与任务授权一致。 */
const desktopDenials = new Map<string, number>()
/**
 * 桌面子 Agent 会话到主对话的映射。
 * 开关关闭或权限失效时，主进程据此通知渲染进程终止真正在运行的那段对话，
 * 而不是只让一次工具调用失败后让模型继续尝试。
 */
const desktopConversationBindings = new Map<
  string,
  { conversationId: string; sourceSessionId: string }
>()
let desktopEmergencyShortcutRegistered = false
/**
 * 桌面权限检查与原生命中操作期间临时拒绝非用户触发的退出。
 * macOS 的屏幕录制授权可能触发系统级 Quit & Reopen；客户端本身不能把这个流程当成正常退出。
 */
let desktopLifecycleGuards = 0
/** 桌面 Agent 被强制终止后的短暂退出保护，容纳后续异步取消与错误回包。 */
let desktopLifecycleGuardUntil = 0

function desktopLifecycleBlocked(): boolean {
  return desktopLifecycleGuards > 0 || Date.now() < desktopLifecycleGuardUntil
}

async function withDesktopLifecycleGuard<T>(task: () => Promise<T> | T): Promise<T> {
  desktopLifecycleGuards += 1
  try {
    return await task()
  } finally {
    desktopLifecycleGuards = Math.max(0, desktopLifecycleGuards - 1)
  }
}

function notifyDesktopAgentUnavailable(
  conversationId: string,
  sourceSessionId: string,
  reason: string,
  permissions: DesktopAgentUnavailableNotice['permissions'] = [],
  restartRequired = false
): void {
  // 错误回包、子 Agent 取消和页面状态更新都是异步的；留出收口窗口，
  // 避免 macOS 上紧接着出现的窗口关闭 / quit 事件把整个应用带走。
  // 保护持续到界面确认已经终止对话并关闭提示；界面确认后再保留一个很短的收尾窗口。
  desktopLifecycleGuardUntil = Math.max(desktopLifecycleGuardUntil, Date.now() + 60_000)
  if (!mainWindow || mainWindow.isDestroyed() || !conversationId) return
  mainWindow.webContents.send('automation:desktop:unavailable', {
    conversationId,
    sourceSessionId,
    reason,
    permissions,
    restartRequired
  } satisfies DesktopAgentUnavailableNotice)
}

/** 桌面托管期间才占用组合键，避免应用在后台时吞掉其他程序的退出 / 取消操作。 */
function registerDesktopEmergencyShortcut(): void {
  if (desktopEmergencyShortcutRegistered) return
  desktopEmergencyShortcutRegistered = globalShortcut.register(
    DESKTOP_EMERGENCY_SHORTCUT,
    emergencyStopDesktopAutomation
  )
  if (!desktopEmergencyShortcutRegistered) {
    console.warn(
      '[DesktopAutomation] Failed to register emergency shortcut:',
      DESKTOP_EMERGENCY_SHORTCUT
    )
  }
}

function unregisterDesktopEmergencyShortcut(): void {
  if (!desktopEmergencyShortcutRegistered) return
  globalShortcut.unregister(DESKTOP_EMERGENCY_SHORTCUT)
  desktopEmergencyShortcutRegistered = false
}

function showDesktopOverlay(): void {
  registerDesktopEmergencyShortcut()
  desktopOverlay.show()
}

function hideDesktopOverlay(): void {
  unregisterDesktopEmergencyShortcut()
  desktopOverlay.hide()
}

function emergencyStopDesktopAutomation(): void {
  console.warn('[DesktopAutomation] Emergency stop shortcut pressed:', DESKTOP_EMERGENCY_SHORTCUT)
  desktopAutomation.emergencyStop()
  hideDesktopOverlay()
  const deniedUntil = Date.now() + DESKTOP_LEASE_TTL
  for (const leaseKey of desktopLeases.keys()) desktopDenials.set(leaseKey, deniedUntil)
  desktopLeases.clear()
  for (const [id, pending] of [...approvals]) {
    if ('desktop' !== pending.kind || !closeApproval(id)) continue
    pending.settle(false)
  }
  if (Notification.isSupported()) {
    new Notification({
      title: '桌面操作已紧急停止',
      body: '已释放鼠标按键并结束当前桌面任务；新的任务可以重新授权。'
    }).show()
  }
}

/** 用户数据比客户端新：提示原因，确认后退出，避免旧结构继续读写新数据 */
async function notifyClientTooNew(error: UserSchemaTooNewError): Promise<void> {
  const options = {
    type: 'warning' as const,
    title: '客户端版本过低',
    message: '无法打开当前用户数据',
    detail:
      `用户数据版本：v${error.dataVersion}\n` +
      `当前客户端支持：v${error.supportedVersion}\n\n` +
      '该用户数据已由更新版本的客户端升级，旧版客户端无法安全读取。\n' +
      '请安装最新版客户端后再打开；请勿删除用户数据。',
    buttons: ['退出应用'],
    defaultId: 0,
    noLink: true
  }
  if (mainWindow && !mainWindow.isDestroyed()) {
    await dialog.showMessageBox(mainWindow, options)
  } else {
    await dialog.showMessageBox(options)
  }
  app.quit()
}

/** 撤下一条申请：从待答复里摘掉，并告诉界面这条作废了（横幅跟着撤掉） */
function closeApproval(id: string): boolean {
  if (!approvals.delete(id)) return false
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('workspace:approval-closed', { id })
  }
  return true
}

/**
 * 停止生成：把这段会话还挂着的申请收掉（按拒绝处理）。
 * 这些申请是命令 / 文件操作等在那一步的凭据，留着就等于「停止」之后还能点允许，
 * 一轮对话被停止后不该再有一步能借它继续跑下去。
 */
function cancelApprovals(conversationId: string, sourceSessionId = ''): void {
  const id = String(conversationId ?? '')
  if (!id) return
  const source = String(sourceSessionId ?? '')
  for (const [requestId, pending] of [...approvals]) {
    if (pending.conversationId !== id) continue
    if (source && pending.sourceSessionId !== source) continue
    if (!closeApproval(requestId)) continue
    pending.settle(false)
  }
}

/** 策略切换或关闭桌面功能时，把正在等答复的桌面任务一起收口，避免留下失效的授权横幅。 */
function settlePendingDesktopApprovals(allow: boolean): void {
  for (const [requestId, pending] of [...approvals]) {
    if ('desktop' !== pending.kind || !closeApproval(requestId)) continue
    if (allow && pending.leaseKey) {
      desktopLeases.set(pending.leaseKey, Date.now() + DESKTOP_LEASE_TTL)
    }
    pending.settle(allow)
  }
}

/**
 * 工作区外的访问申请：推给界面问一次并等答复。
 * 窗口不在（或已销毁）直接按拒绝处理，超时同样按拒绝处理，不让一轮对话悬着。
 */
function requestApproval(request: WorkspaceApproval | DesktopApprovalRequest): Promise<boolean> {
  if (quitHandshakeStarted || !mainWindow || mainWindow.isDestroyed()) return Promise.resolve(false)
  return new Promise<boolean>((settle) => {
    const isDesktop = 'desktop' === (request as DesktopApprovalRequest).kind
    const leaseKey = String(request.sourceSessionId || request.conversationId || '')
    let settled = false
    const finish = (allow: boolean): void => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      if (isDesktop && leaseKey) {
        if (allow) desktopDenials.delete(leaseKey)
        else desktopDenials.set(leaseKey, Date.now() + DESKTOP_LEASE_TTL)
      }
      settle(allow)
    }
    const timer = setTimeout(() => {
      // 等到超时按拒绝处理：顺手告诉界面这条申请作废了，别让横幅与会话列表上的点一直挂着
      if (closeApproval(request.id)) finish(false)
    }, APPROVAL_TIMEOUT)
    approvals.set(request.id, {
      kind: isDesktop ? 'desktop' : 'workspace',
      conversationId: String(request.conversationId ?? ''),
      sourceSessionId: String(request.sourceSessionId ?? ''),
      leaseKey,
      settle: finish
    })
    mainWindow!.webContents.send('workspace:approval-request', request)
  })
}

/**
 * 拖出窗口时跟着光标走的那张小图：应用图标缩到 32px。
 * 原图是几百像素的方图，按原样交给系统会大得压住指针；读不出来时退回原图路径，让 Electron 自己再试一次。
 */
let dragImage: NativeImage | string | null = null

function dragIcon(): NativeImage | string {
  if (!dragImage) {
    const image = nativeImage.createFromPath(icon)
    dragImage = image.isEmpty() ? icon : image.resize({ width: 32, height: 32 })
  }
  return dragImage
}

function closeAgentWindows(): void {
  for (const window of agentWindows.values()) {
    if (!window.isDestroyed()) window.close()
  }
  agentWindows.clear()
  agentSnapshots.clear()
}

function openAgentWindow(sessionId: string): BrowserWindow {
  const existing = agentWindows.get(sessionId)
  if (existing && !existing.isDestroyed()) {
    if (existing.isMinimized()) existing.restore()
    existing.show()
    existing.focus()
    return existing
  }

  const owner = mainWindow
  const bounds = owner?.getBounds()
  const area = bounds
    ? screen.getDisplayMatching(bounds).workArea
    : screen.getPrimaryDisplay().workArea
  const width = Math.min(820, Math.max(520, area.width - 40))
  const height = Math.min(760, Math.max(460, area.height - 40))
  let x = bounds ? bounds.x + bounds.width + 12 : area.x + Math.round((area.width - width) / 2)
  let y = bounds ? Math.max(area.y, Math.min(bounds.y, area.y + area.height - height)) : area.y + 40
  if (x + width > area.x + area.width) {
    x = bounds
      ? Math.max(area.x, Math.min(bounds.x - width - 12, area.x + area.width - width))
      : area.x + Math.round((area.width - width) / 2)
    y = bounds ? Math.min(area.y + area.height - height, bounds.y + 24) : y
  }

  const detailWindow = new BrowserWindow({
    x,
    y,
    width,
    height,
    minWidth: 520,
    minHeight: 460,
    show: false,
    autoHideMenuBar: true,
    title: '子 Agent 执行详情',
    backgroundColor: '#f5f7fa',
    ...(process.platform === 'linux' ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })
  agentWindows.set(sessionId, detailWindow)

  detailWindow.once('ready-to-show', () => {
    detailWindow.show()
    const snapshot = agentSnapshots.get(sessionId)
    if (snapshot) detailWindow.webContents.send('agent-window:update', snapshot)
  })
  detailWindow.on('closed', () => {
    if (agentWindows.get(sessionId) === detailWindow) agentWindows.delete(sessionId)
    agentSnapshots.delete(sessionId)
  })
  detailWindow.webContents.setWindowOpenHandler(({ url }) => {
    openSafeExternalUrl(url)
    return { action: 'deny' }
  })

  const hash = `/agent-detail?sessionId=${encodeURIComponent(sessionId)}`
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    void detailWindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}#${hash}`)
  } else {
    void detailWindow.loadFile(join(__dirname, '../renderer/index.html'), { hash })
  }
  return detailWindow
}

function createWindow(): void {
  // 登录页是启动后的默认落点，先按登录尺寸创建。已登录用户会在路由守卫里
  // 校验会话后切到主界面尺寸，避免退出登录时先露出一帧 900×670 的大窗口。
  mainWindowMode = 'login'
  pinned = false
  savedBounds = null
  filesPanelApplied = 0
  filesPanelShiftX = 0
  mainWindow = new BrowserWindow({
    width: LOGIN_WINDOW_SIZE.width,
    height: LOGIN_WINDOW_SIZE.height,
    backgroundColor: WINDOW_BACKGROUND_COLOR,
    show: false,
    resizable: false,
    frame: false,
    autoHideMenuBar: true,
    ...(process.platform === 'linux' ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow!.show()
  })

  mainWindow.on('maximize', () => {
    mainWindow?.webContents.send('window:maximize-change', true)
  })

  mainWindow.on('unmaximize', () => {
    mainWindow?.webContents.send('window:maximize-change', false)
    // 最大化时文件区没法把窗口再加宽；还原后按当前展开状态补上（期间可能被收起 / 展开过）
    applyFilesPanelWidth()
  })

  mainWindow.on('close', (event) => {
    if (desktopLifecycleBlocked()) {
      event.preventDefault()
      console.warn('[DesktopAutomation] Blocked automatic window close during desktop operation')
      return
    }
    if (quitHandshakeDone) return
    event.preventDefault()
    requestQuitHandshake()
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    openSafeExternalUrl(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer base on electron-vite cli.
  // Load the remote URL for development or the local html file for production.
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }

  schedule.attach(mainWindow)
  // 页面重载或渲染进程退出时先停掉调度，等新的渲染进程重新同步任务后再挂上
  mainWindow.webContents.on('did-start-navigation', (_event, _url, _isInPlace, isMainFrame) => {
    if (isMainFrame) schedule.stop()
  })
  mainWindow.webContents.on('render-process-gone', () => schedule.stop())
  mainWindow.on('closed', () => {
    closeAgentWindows()
    mainWindow = null
    // 窗口关闭即视为客户端停止：不在后台继续跑定时任务
    schedule.stop()
  })
}

ipcMain.handle('window:set-login-size', () => {
  applyLoginWindowMode()
})

ipcMain.handle('window:set-normal-size', () => {
  resetMainWindowSize()
})

ipcMain.handle('window:minimize', () => {
  mainWindow?.minimize()
})

ipcMain.handle('window:maximize', () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow?.maximize()
  }
})

ipcMain.handle('window:close', () => {
  mainWindow?.close()
})

/** 强制升级弹窗的「退出应用」：跨平台退出整个进程，而不是只关闭窗口 */
ipcMain.handle('app:quit', () => {
  app.quit()
})

/** 渲染进程已经写完退出检查点；此时才允许真正退出并关闭数据库 */
ipcMain.handle('app:quit-ready', () => {
  finishQuitHandshake()
})

ipcMain.handle('window:is-maximized', () => {
  return mainWindow?.isMaximized() ?? false
})

let savedBounds: { x: number; y: number; width: number; height: number } | null = null

/**
 * 右侧文件区占用的窗口宽度：文件区展开不是从对话列里分地方，而是把窗口整体加宽。
 *
 * `applied` 记「当前窗口里已经为文件区让出的宽度」，收起时按它缩回（屏幕放不下时
 * 实际只加宽了一部分，不能拿用户设定的宽度直接减）。展开时若右边缘会越过屏幕工作区，
 * 窗口向左让位并记下位移，收起时再把位置还原。
 */
let filesPanelVisible = false
let filesPanelWidth = 0
let filesPanelApplied = 0
let filesPanelShiftX = 0
let pinned = false
let pinnedPanelApplied = 0
let pinnedPanelShiftX = 0

/** 按文件区的当前意愿对齐窗口宽度：展开加宽、收起缩回，置顶与最大化期间不动 */
function applyFilesPanelWidth(): void {
  if (mainWindowMode !== 'main' || !mainWindow || pinned || mainWindow.isMaximized()) return

  const target = filesPanelVisible ? Math.max(0, Math.round(filesPanelWidth)) : 0
  const delta = target - filesPanelApplied
  if (!delta) return

  const bounds = mainWindow.getBounds()
  const area = screen.getDisplayMatching(bounds).workArea
  // 窗口最多铺满当前屏幕的工作区：屏幕本身放不下时，文件区只能挤一点对话列
  const width = Math.min(Math.max(bounds.width + delta, 360), area.width)
  const applied = width - bounds.width
  if (!applied) return

  let x = bounds.x
  if (applied > 0) {
    // 优先向右长；右边不够就整窗左移，保证加宽后的右边缘仍在屏幕内
    const right = area.x + area.width
    if (x + width > right) {
      const nextX = Math.max(area.x, right - width)
      filesPanelShiftX += x - nextX
      x = nextX
    }
  } else if (!target) {
    // 完全收起时还回展开前的水平位置（用户中途拖过窗口也不至于跑出屏幕）
    x += filesPanelShiftX
    filesPanelShiftX = 0
    x = Math.max(area.x, Math.min(x, area.x + area.width - width))
  }

  mainWindow.setBounds({ x, y: bounds.y, width, height: bounds.height }, false)
  filesPanelApplied += applied
}

/**
 * 窗口模式切换前先撤掉最大化 / 置顶状态。Windows 在最大化状态下可能忽略
 * setSize，登录页就会沿用主界面的全屏尺寸。
 */
function resizeMainWindow(size: { width: number; height: number }, resizable: boolean): void {
  if (!mainWindow || mainWindow.isDestroyed()) return
  if (!mainWindow.isResizable()) mainWindow.setResizable(true)
  if (mainWindow.isFullScreen()) mainWindow.setFullScreen(false)
  if (mainWindow.isMaximized()) mainWindow.unmaximize()
  // 原生窗口会先放大，WebContents 的下一帧可能稍晚才覆盖新增区域。
  // 两者统一用页面底色，避免用户看到黑底或其他旧背景闪一下。
  mainWindow.setBackgroundColor(WINDOW_BACKGROUND_COLOR)
  mainWindow.setSize(size.width, size.height)
  mainWindow.center()
  mainWindow.setResizable(resizable)
}

function applyLoginWindowMode(): void {
  if (!mainWindow || mainWindow.isDestroyed()) return
  mainWindowMode = 'login'
  pinned = false
  savedBounds = null
  mainWindow.setAlwaysOnTop(false)
  filesPanelApplied = 0
  filesPanelShiftX = 0
  pinnedPanelApplied = 0
  pinnedPanelShiftX = 0
  resizeMainWindow(LOGIN_WINDOW_SIZE, false)
}

/**
 * 主界面窗口的基准尺寸。登录回来后文件区若还开着，以这个尺寸重新加宽，
 * 别把登录页那次固定尺寸算成文件区的占位。
 */
function resetMainWindowSize(): void {
  if (!mainWindow || mainWindow.isDestroyed()) return
  mainWindowMode = 'main'
  filesPanelApplied = 0
  filesPanelShiftX = 0
  resizeMainWindow(MAIN_WINDOW_SIZE, true)
  applyFilesPanelWidth()
}

ipcMain.handle('window:toggle-pin', (_event, pin: boolean) => {
  if (!mainWindow) return

  if (pin) {
    const bounds = mainWindow.getBounds()
    savedBounds = { x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height }
    // 钉住期间窗口固定 400×600：先把文件区已经占用的宽度收进「钉住前」这份记录，
    // 取消钉住时再按文件区当前状态对齐（期间只能改意愿，不能改窗口）
    pinnedPanelApplied = filesPanelApplied
    pinnedPanelShiftX = filesPanelShiftX
    pinned = true
    filesPanelApplied = 0
    filesPanelShiftX = 0
    mainWindow.setAlwaysOnTop(true)
    mainWindow.setSize(400, 600)
    mainWindow.center()
  } else {
    mainWindow.setAlwaysOnTop(false)
    pinned = false
    if (savedBounds) {
      mainWindow.setBounds(savedBounds)
      savedBounds = null
      filesPanelApplied = pinnedPanelApplied
      filesPanelShiftX = pinnedPanelShiftX
    }
    applyFilesPanelWidth()
  }
})

/** 右侧文件区展开 / 收起：界面只报意愿与宽度，窗口怎么让位由主进程算 */
ipcMain.handle('window:files-panel', (_event, request: { visible?: boolean; width?: number }) => {
  filesPanelVisible = true === request?.visible
  const width = Number(request?.width)
  if (Number.isFinite(width) && width > 0) filesPanelWidth = Math.round(width)
  applyFilesPanelWidth()
})

app.whenReady().then(async () => {
  /**
   * 运行目录：系统配置（`userdata/config.toml`）、用户库（`userdata/<userId>/`）、
   * 本地图标（`userdata/icons/`）与技能目录（`skills/`）都挂在它下面，各 init 自己再拼子目录。
   *
   * - 开发模式：项目根目录（`userdata/`、`skills/` 已在 .gitignore 里），改完即见；
   * - 生产环境：用户主目录下的 `.fs-bot`。**不往安装目录里写**——Windows 的 Program Files 未必可写，
   *   macOS 的「安装目录」更是 .app 包内部：往包里写会破坏代码签名，应用更新换整包时数据也跟着丢。
   */
  const runtimeDir = is.dev ? app.getAppPath() : join(app.getPath('home'), '.fs-bot')

  if (is.dev) {
    const c = (code: number, s: string): string => `\x1b[${code}m${s}\x1b[0m`
    const pad = (label: string, value: string): string =>
      `  ${c(36, label.padEnd(14))}: ${c(32, value)}`

    console.log(c(90, '-- runtime info ------------------------------------------'))
    console.log(pad('runtimeDir', runtimeDir))
    console.log(pad('__dirname', __dirname))
    console.log(pad('cwd', process.cwd()))
    console.log(c(90, '-----------------------------------------------------------'))
  }

  // Set app user model id for windows
  electronApp.setAppUserModelId('com.iisquare.fs.bot')

  // Default open or close DevTools by F12 in development
  // and ignore CommandOrControl + R in production.
  // see https://github.com/alex8088/electron-toolkit/tree/master/packages/utils
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // Database initialization
  const config = ConfigStore.getInstance()
  config.init(runtimeDir)
  // 更新状态跨版本保留：新版本启动并完成界面健康确认后才算安装成功
  initUpdateState(runtimeDir)
  // 技能目录初始化：技能包解压到这里，页面按目录名展示已安装技能
  skills.init(runtimeDir)
  // 图标目录初始化：选中的本地图片复制到这里，界面按 app-icon://<文件名> 引用
  initIcons(runtimeDir)
  // 浏览器自动化：登录态、临时截图与下载都放在运行目录下
  browserAutomation.init(runtimeDir)
  // 桌面自动化：截图临时文件放在用户运行目录，不进入安装目录
  desktopAutomation.init(runtimeDir)
  desktopOverlay.setStopHandler(emergencyStopDesktopAutomation)
  ipcMain.on('desktop-overlay:set-interactive', (event, interactive) => {
    desktopOverlay.setInteractive(event.sender, interactive === true)
  })
  ipcMain.on('desktop-overlay:stop', (event) => {
    desktopOverlay.requestStop(event.sender)
  })

  ipcMain.handle('agent-window:open', (event, request: { sessionId?: string }) => {
    if (!mainWindow || event.sender !== mainWindow.webContents) {
      throw new LocalizedError(
        'AGENT_WINDOW_SOURCE_INVALID',
        '只能从主对话窗口打开子 Agent 详情',
        'Agent details can only be opened from the main conversation window'
      )
    }
    const sessionId = String(request?.sessionId ?? '').trim()
    if (!sessionId) {
      throw new LocalizedError(
        'AGENT_SESSION_ID_REQUIRED',
        '缺少子 Agent 会话标识',
        'The child-agent session ID is required'
      )
    }
    const rows = db.select('agent_sessions', { id: sessionId }).data as Record<string, unknown>[]
    if (!rows[0]) {
      throw new LocalizedError(
        'AGENT_SESSION_NOT_FOUND',
        '找不到子 Agent 会话',
        `Child-agent session not found: ${sessionId}`
      )
    }
    const window = openAgentWindow(sessionId)
    return { opened: true, sessionId, focused: !window.isDestroyed() }
  })

  ipcMain.on('agent-window:publish', (event, request: { sessionId?: string }) => {
    if (!mainWindow || event.sender !== mainWindow.webContents) return
    const sessionId = String(request?.sessionId ?? '').trim()
    if (!sessionId || !agentWindows.has(sessionId)) return
    const payload = {
      sessionId,
      interaction: request && 'interaction' in request ? request.interaction : null
    }
    agentSnapshots.set(sessionId, payload)
    const window = agentWindows.get(sessionId)
    if (window && !window.isDestroyed()) {
      window.webContents.send('agent-window:update', payload)
    }
  })

  ipcMain.handle('agent-window:close-self', (event) => {
    const window = BrowserWindow.fromWebContents(event.sender)
    window?.close()
  })

  ipcMain.handle('agent-window:close-all', (event) => {
    if (!mainWindow || event.sender !== mainWindow.webContents) return
    closeAgentWindows()
  })

  ipcMain.handle('db:init-user', async (_event, request: DbInitUserRequest) => {
    try {
      db.initUser(request.serial, request.userId, request.dbSecret, runtimeDir)
      return { success: true }
    } catch (error) {
      if (error instanceof UserSchemaTooNewError) {
        await notifyClientTooNew(error)
        return { success: false }
      }
      throw error
    }
  })

  ipcMain.handle('db:select', (_event, request: DbSelectRequest) => {
    return db.select(request.table, request.where, request.orderBy, request.limit)
  })

  ipcMain.handle('db:insert', (_event, request: DbInsertRequest) => {
    return db.insert(request.table, request.data)
  })

  ipcMain.handle('db:update', (_event, request: DbUpdateRequest) => {
    return db.update(request.table, request.where, request.data)
  })

  ipcMain.handle('db:upsert', (_event, request: DbUpsertRequest) => {
    return db.upsert(request.table, request.data)
  })

  ipcMain.handle('db:save-messages', (_event, request: DbSaveMessagesRequest) => {
    return db.saveMessages(
      request.conversationId,
      request.messages,
      request.leafId,
      request.updatedAt
    )
  })

  ipcMain.handle('db:delete', (_event, request: DbDeleteRequest) => {
    return db.remove(request.table, request.where)
  })

  ipcMain.handle('db:delete-batch', (_event, request: DbDeleteBatchRequest) => {
    return db.removeBatch(request.deletes)
  })

  /**
   * 关键词检索：历史对话（记忆）按标题与消息正文找行，由主进程一条 SQL 查完。
   * mode 传 `segments` 时按空白切成多段匹配（多行片段、换行 / 缩进对不上的兜底口径）。
   */
  ipcMain.handle('db:search', (_event, request: DbSearchRequest) => {
    return db.search(request.table, request.columns, request.keyword, {
      where: request.where,
      in: request.in,
      orderBy: request.orderBy,
      limit: request.limit,
      mode: request.mode
    })
  })

  ipcMain.handle('config:get-settings', () => {
    return config.getSettings()
  })

  /** 版本信息：客户端安装版本与当前用户库的 schema 版本，供系统设置页展示 */
  ipcMain.handle('app:versions', () => {
    return {
      clientVersion: app.getVersion(),
      dataVersion: db.userSchemaVersion,
      platform: process.platform,
      architecture: process.arch
    }
  })

  /** 客户端身份与更新渠道：业务请求头、灰度分桶和更新检查共用 */
  ipcMain.handle('app:client-meta', () => {
    return currentClientMeta()
  })

  /** 下载安装包：主进程负责写入临时文件、大小 / SHA-512 校验与启动安装器 */
  ipcMain.handle('app:update:download', async (event, request: UpdateInstallRequest) => {
    return downloadAndInstallUpdate(request, (progress) => {
      if (!event.sender.isDestroyed()) event.sender.send('app:update:progress', progress)
    })
  })

  ipcMain.handle('app:update:status', () => updateState())
  ipcMain.handle('app:update:control', () => downloadControlStatus())
  ipcMain.handle('app:update:pause', () => pauseUpdateDownload())
  ipcMain.handle('app:update:cancel', () => cancelUpdateDownload())

  ipcMain.handle('app:update:healthy', () => reportUpdateHealthy())

  ipcMain.handle('app:auto-start:get', () => {
    return getAutoStartStatus()
  })

  ipcMain.handle('app:auto-start:set', (_event, request: { enabled?: boolean }) => {
    return setAutoStart(request?.enabled === true)
  })

  ipcMain.handle('config:update-setting', (_event, request: ConfigUpdateSettingRequest) => {
    if (request.key === 'autoStart') {
      throw new LocalizedError(
        'AUTOSTART_SYSTEM_MANAGED',
        '开机启动由系统登录项管理，请使用系统设置页的开关',
        'Auto-start is managed by the system login-item settings; use the system settings page'
      )
    }
    config.updateSetting(request.key, request.value)
    return { success: true }
  })

  ipcMain.handle('config:login-history:list', () => {
    return config.listLoginHistory()
  })

  ipcMain.handle('config:login-history:save', (_event, request: ConfigLoginHistoryRequest) => {
    return config.saveLoginHistory(request.serial)
  })

  ipcMain.handle('config:login-history:remove', (_event, request: ConfigLoginHistoryRequest) => {
    return config.removeLoginHistory(request.serial)
  })

  /** 定时调度：渲染进程只同步启用任务的标识 / cron / 开关，到点再由主进程通知执行 */
  ipcMain.handle('schedule:sync', (_event, tasks: unknown) => {
    schedule.sync(tasks)
  })

  ipcMain.handle('schedule:stop', () => {
    schedule.stop()
  })

  /**
   * 项目工作区：文件操作都在主进程落地，渲染进程只拿结果。
   *
   * 权限清单不接受渲染进程上报，而是按项目从用户库里读该项目的声明：
   * 会话按项目创建，切会话 / 多项目并行时各自用自己项目的口径，互不覆盖。
   */
  workspace.setProjectRootsResolver((projectId) => {
    if (!projectId) return []
    const rows = db.select('projects', { id: projectId }).data as Record<string, unknown>[]
    const row = rows[0]
    if (!row) {
      throw new LocalizedError(
        'PROJECT_NOT_FOUND',
        `项目不存在或已删除：${projectId}`,
        `Project not found or deleted: ${projectId}`
      )
    }
    return parseWorkspaceRoots(row['workspace'])
  })

  /**
   * 项目环境变量：同样按项目从用户库读，只进本机命令的进程环境（见 WorkspaceManager.exec）。
   * 读不到（项目已删、列里是坏 JSON、变量名不合规）就给空——它们是附加项，
   * 不该让一条命令因为一份读不出来的配置执行失败。
   */
  workspace.setProjectEnvResolver((projectId) => {
    if (!projectId) return []
    const rows = db.select('projects', { id: projectId }).data as Record<string, unknown>[]
    return parseProjectEnvVars(rows[0]?.['env_vars'])
  })

  // 工作区外文件的访问策略与询问通道：策略随系统设置持久化，申请推给界面弹窗确认
  workspace.setPolicy(config.getSettings()['workspacePolicy'])
  workspace.setApprovalHandler((request) => requestApproval(request))

  ipcMain.handle('workspace:policy:get', () => {
    return workspace.getPolicy()
  })

  ipcMain.handle('workspace:policy:set', (_event, request: WorkspacePolicyRequest) => {
    const policy = workspace.setPolicy(request?.policy)
    config.updateSetting('workspacePolicy', policy)
    return policy
  })

  ipcMain.handle('workspace:approval:response', (_event, request: WorkspaceApprovalResponse) => {
    const pending = approvals.get(String(request?.id ?? ''))
    // 工作区的「始终允许」才修改工作区策略。桌面授权只给当前子任务一个短期租约，
    // 不能因为一次点击就把整个电脑永久设为自动批准。
    if (request?.allow && request?.always && 'workspace' === pending?.kind) {
      const policy = workspace.setPolicy('auto')
      config.updateSetting('workspacePolicy', policy)
    }
    // 桌面授权按“任务”生效：横幅提供拒绝或允许本任务，不再为每个动作重复确认。
    if (request?.allow && 'desktop' === pending?.kind && pending.leaseKey) {
      desktopLeases.set(pending.leaseKey, Date.now() + DESKTOP_LEASE_TTL)
      desktopAutomation.clearEmergency()
    }
    if (pending) {
      approvals.delete(String(request?.id ?? ''))
      pending.settle(request?.allow === true)
    }
    return { success: true }
  })

  /**
   * 停止生成：收掉这段会话还挂着的本地活儿——等答复的申请按拒绝处理、在跑的命令立刻终止。
   *
   * 界面按停止时断的是模型流（见 chat store 的 stop），流一断就收不到后续增量；
   * 但工具执行这一刻没有流可断（本地命令跑在主进程、等答复的申请悬在主进程），
   * 只有把这两样也收掉，「停止」才算真的停住：命令不会跑到超时、申请也不会在停止后
   * 被点一下就把整轮对话接着跑下去。返回被终止的命令条数（界面不关心，日志好查）。
   */
  ipcMain.handle('workspace:cancel', (_event, request: { conversationId?: string }) => {
    const conversationId = String(request?.conversationId ?? '')
    let approvalConversationId = conversationId
    let sourceSessionId = ''
    if (conversationId) {
      try {
        const rows = db.select('agent_sessions', { id: conversationId }).data as Record<
          string,
          unknown
        >[]
        if (rows[0]) {
          approvalConversationId = String(rows[0]['parent_conversation_id'] ?? conversationId)
          sourceSessionId = conversationId
        }
      } catch {
        // 主对话自己的任务不会出现在 agent_sessions 中。
      }
    }
    cancelApprovals(approvalConversationId, sourceSessionId)
    browserAutomation.cancel(conversationId)
    desktopAutomation.cancel(sourceSessionId || conversationId)
    hideDesktopOverlay()
    const desktopSessionKey = sourceSessionId || conversationId
    desktopLeases.delete(desktopSessionKey)
    desktopDenials.delete(desktopSessionKey)
    for (const [key, binding] of desktopConversationBindings) {
      if (key === conversationId || binding.conversationId === approvalConversationId) {
        desktopConversationBindings.delete(key)
      }
    }
    return { stopped: workspace.cancel(conversationId) }
  })

  /** 浏览器自动化：模型按快照 ref 操作页面，执行器自动等待、处理弹窗与记录下载 */
  ipcMain.handle('automation:browser', async (_event, request: BrowserOperationRequest) => {
    const uploadPaths: string[] = []
    if ('upload' === request.action) uploadPaths.push(String(request.args?.path ?? '').trim())
    if ('perform' === request.action && Array.isArray(request.args?.steps)) {
      for (const step of request.args.steps) {
        if (!step || 'object' !== typeof step) continue
        const row = step as Record<string, unknown>
        if ('upload' === row['action']) uploadPaths.push(String(row['path'] ?? '').trim())
      }
    }
    const browserSessionId = String(request.conversationId ?? '')
    let approvalConversationId = browserSessionId
    if (browserSessionId) {
      try {
        const rows = db.select('agent_sessions', { id: browserSessionId }).data as Record<
          string,
          unknown
        >[]
        approvalConversationId = String(rows[0]?.['parent_conversation_id'] ?? browserSessionId)
      } catch {
        // 主对话自己的浏览器会话不在 agent_sessions 中，沿用原会话标识。
      }
    }
    for (const path of uploadPaths.filter(Boolean)) {
      await workspace.authorizeRead(
        String(request.projectId ?? ''),
        path,
        approvalConversationId,
        browserSessionId
      )
    }
    return browserAutomation.run(request)
  })

  /**
   * 桌面自动化：首步由用户授权，选中「允许本任务」后给当前子 Agent 一个短期租约。
   * 桌面鼠标和键盘是全局资源，因此执行器与审批都在主进程，渲染进程拿不到直接注入能力。
   */
  ipcMain.handle(
    'automation:desktop',
    async (_event, request: DesktopOperationRequest): Promise<unknown> => {
      const status = desktopAutomation.status()
      const sessionId = String(request?.sourceSessionId ?? '').trim()
      let parentConversationId = String(request?.conversationId ?? '').trim()
      if (sessionId) {
        try {
          const rows = db.select('agent_sessions', { id: sessionId }).data as Record<
            string,
            unknown
          >[]
          parentConversationId = String(rows[0]?.['parent_conversation_id'] ?? parentConversationId)
        } catch {
          // 会话可能刚结束；沿用工具调用带上来的会话标识。
        }
      }
      const leaseKey = sessionId || parentConversationId
      if (parentConversationId) {
        desktopConversationBindings.set(leaseKey, {
          conversationId: parentConversationId,
          sourceSessionId: sessionId
        })
      }
      if (!status.supported) {
        const reason = `${status.reason}。Desktop Agent 对话已自动终止。`
        notifyDesktopAgentUnavailable(parentConversationId, sessionId, reason)
        return { text: reason }
      }
      if (true !== config.getSettings()['desktopAutomationEnabled']) {
        const reason = '桌面操作已关闭，Desktop Agent 对话已自动终止。请到系统设置中开启后再试。'
        notifyDesktopAgentUnavailable(parentConversationId, sessionId, reason)
        return { text: reason }
      }
      const runtimePermission = await desktopAutomation.checkPermissionsForRun()
      if (!runtimePermission.permissionsReady) {
        const reason = `${runtimePermission.reason}。Desktop Agent 对话已自动终止。`
        notifyDesktopAgentUnavailable(
          parentConversationId,
          sessionId,
          reason,
          runtimePermission.permissions,
          runtimePermission.restartRequired
        )
        return { text: reason }
      }
      if ('close' === request.action) {
        const result = await desktopAutomation.run(request)
        hideDesktopOverlay()
        desktopLeases.delete(leaseKey)
        desktopDenials.delete(leaseKey)
        desktopConversationBindings.delete(leaseKey)
        return result
      }
      const deniedUntil = desktopDenials.get(leaseKey) ?? 0
      if (deniedUntil > Date.now()) {
        throw new LocalizedError(
          'DESKTOP_TASK_DENIED',
          '当前桌面任务已被拒绝或停止，不能再次申请授权；不要重试，请结束任务并告知用户未执行',
          'Desktop automation was denied or stopped; do not request authorization again'
        )
      }
      if (deniedUntil) desktopDenials.delete(leaseKey)
      const expiresAt = desktopLeases.get(leaseKey) ?? 0
      if (expiresAt <= Date.now()) {
        desktopLeases.delete(leaseKey)
        const policy = normalizeDesktopApprovalPolicy(config.getSettings()['desktopApprovalPolicy'])
        if ('auto' === policy) {
          desktopLeases.set(leaseKey, Date.now() + DESKTOP_LEASE_TTL)
          desktopAutomation.clearEmergency()
        } else {
          const labels: Record<DesktopAction, string> = {
            windows: '查看可用窗口',
            snapshot: '查看屏幕',
            focus: '切换窗口',
            click: '鼠标点击',
            type_text: '输入文字',
            press: '按键',
            scroll: '滚动画面',
            drag: '鼠标拖拽',
            wait: '等待画面变化',
            screenshot: '截取屏幕',
            close: '结束桌面会话'
          }
          const action = String(request?.action ?? '') as DesktopAction
          const approval: DesktopApprovalRequest = {
            id: randomUUID(),
            kind: 'desktop',
            projectId: String(request?.projectId ?? ''),
            path: '',
            need: 'desktop',
            action,
            title: '桌面操作授权',
            detail:
              `模型准备执行「${labels[action] ?? action}」。授权期间，当前窗口截图会发送给所选模型，` +
              '鼠标和键盘会直接操作这台电脑。',
            conversationId: parentConversationId,
            sourceSessionId: sessionId
          }
          const allowed = await requestApproval(approval)
          if (!allowed) {
            throw new LocalizedError(
              'DESKTOP_AUTH_DENIED',
              '当前桌面任务未获授权；不要重试或再次申请，请结束任务并告知用户未执行',
              'Desktop automation authorization was denied; stop the task without retrying'
            )
          }
          desktopAutomation.clearEmergency()
        }
      }
      return withDesktopLifecycleGuard(async () => {
        showDesktopOverlay()
        try {
          return await desktopAutomation.run(request)
        } catch (error) {
          if (!isDesktopPermissionFailure(error)) throw error
          const detail = error instanceof Error ? error.message : String(error)
          const permissions = runtimePermission.permissions.map((item) => {
            const failedAccessibility =
              'accessibility' === item.key && /(?:辅助功能|accessibility)/i.test(detail)
            const failedScreen =
              'screenRecording' === item.key &&
              /(?:屏幕录制|screen recording|capture failed)/i.test(detail)
            return failedAccessibility || failedScreen
              ? { ...item, granted: false, state: 'denied' as const }
              : item
          })
          const reason = `${detail}。Desktop Agent 对话已自动终止。`
          notifyDesktopAgentUnavailable(
            parentConversationId,
            sessionId,
            reason,
            permissions,
            runtimePermission.restartRequired
          )
          hideDesktopOverlay()
          return { text: reason }
        }
      })
    }
  )

  const desktopAutomationStatus = (
    status: DesktopAutomationSupportStatus = desktopAutomation.status()
  ): DesktopAutomationStatus => {
    return {
      ...status,
      enabled: true === config.getSettings()['desktopAutomationEnabled'],
      approvalPolicy: normalizeDesktopApprovalPolicy(config.getSettings()['desktopApprovalPolicy'])
    }
  }

  ipcMain.handle('automation:desktop:status', () => {
    return desktopAutomationStatus()
  })

  ipcMain.handle('automation:desktop:release-unavailable', () => {
    desktopLifecycleGuardUntil = Math.max(desktopLifecycleGuardUntil, Date.now() + 3_000)
    return true
  })

  ipcMain.handle(
    'automation:desktop:open-permission-settings',
    (_event, request: { permission?: DesktopPermissionKey }) => {
      return desktopAutomation.openPermissionSettings(request?.permission as DesktopPermissionKey)
    }
  )

  ipcMain.handle(
    'automation:desktop:set-enabled',
    async (_event, request: { enabled?: boolean }) => {
      return withDesktopLifecycleGuard(async () => {
        const shouldEnable = request?.enabled === true
        const support = shouldEnable
          ? await desktopAutomation.checkPermissionsForEnable()
          : desktopAutomation.status()
        if (!support.supported && shouldEnable) {
          throw new LocalizedError(
            'DESKTOP_UNSUPPORTED',
            support.reason,
            `Desktop automation is not supported on ${process.platform}`
          )
        }
        config.updateSetting('desktopAutomationEnabled', shouldEnable)
        // 状态切换本身不触发权限申请；这段保护只用来隔离 macOS 后续异步事件。
        desktopLifecycleGuardUntil = Math.max(desktopLifecycleGuardUntil, Date.now() + 2_000)
        if (!shouldEnable) {
          const conversations = new Set(
            [...desktopConversationBindings.values()].map((item) => item.conversationId)
          )
          for (const conversationId of conversations) {
            notifyDesktopAgentUnavailable(
              conversationId,
              '',
              '桌面操作已关闭，Desktop Agent 对话已自动终止。请到系统设置中重新开启后再试。'
            )
          }
          desktopConversationBindings.clear()
          settlePendingDesktopApprovals(false)
          desktopLeases.clear()
          desktopDenials.clear()
          desktopAutomation.cancel('')
          hideDesktopOverlay()
        }
        return desktopAutomationStatus(support)
      })
    }
  )

  ipcMain.handle(
    'automation:desktop:set-approval-policy',
    (_event, request: { policy?: string }) => {
      const policy = normalizeDesktopApprovalPolicy(request?.policy)
      config.updateSetting('desktopApprovalPolicy', policy)
      // 新策略从下一次桌面调用开始生效；已有租约和拒绝记录不能跨策略继承。
      desktopLeases.clear()
      desktopDenials.clear()
      if ('auto' === policy) settlePendingDesktopApprovals(true)
      return policy
    }
  )

  /**
   * 选一个本机路径（取消返回空串）：kind 决定选文件还是目录、title 是对话框标题（不给就按 kind 出一句通用的）。
   * 一次只选一个——这类入口都只有一个落点；要选一批走 `workspace:pick-paths`。
   */
  ipcMain.handle('workspace:pick', async (_event, request?: { kind?: string; title?: string }) => {
    if (!mainWindow) return ''
    const file = 'file' === String(request?.kind ?? '')
    const result = await dialog.showOpenDialog(mainWindow, {
      title: String(request?.title ?? '').trim() || (file ? '选择文件' : '选择目录'),
      properties: file ? ['openFile', 'showHiddenFiles'] : ['openDirectory', 'createDirectory']
    })
    return result.canceled || result.filePaths.length < 1 ? '' : result.filePaths[0]
  })

  /**
   * 引用本地文件 / 文件夹：选择时不限制路径，也不做权限判定——
   * 真正的校验放在读取（引用内容进上下文）与写入（模型调工具）时，
   * 按项目声明的目录权限与工作区外访问策略放行。
   *
   * Windows / Linux 的原生对话框不能同时选文件与文件夹，所以拆成两种入口：
   * kind 为 file 选文件、directory 选文件夹（两者都不设类型过滤）。
   */
  ipcMain.handle('workspace:pick-paths', async (_event, request: { kind?: string }) => {
    if (!mainWindow) return []
    const directory = 'directory' === String(request?.kind ?? '')
    const result = await dialog.showOpenDialog(mainWindow, {
      title: directory ? '引用本地文件夹' : '引用本地文件',
      properties: directory
        ? (
            ['openDirectory', 'createDirectory', 'multiSelections', 'showHiddenFiles'] as const
          ).slice()
        : (
            ['openFile', 'multiSelections', 'showHiddenFiles', 'treatPackageAsDirectory'] as const
          ).slice()
    })
    return result.canceled ? [] : result.filePaths
  })

  /** 引用一段本地路径：文件给内容、目录给清单，鉴权与访问策略同在主进程完成 */
  ipcMain.handle('workspace:reference', (_event, request: WorkspaceReadRequest) => {
    return workspace.reference(request.projectId, request.path, request.conversationId)
  })

  ipcMain.handle('workspace:list', (_event, request: WorkspaceReadRequest) => {
    return workspace.list(request.projectId, request.path, request.conversationId)
  })

  ipcMain.handle('workspace:read', (_event, request: WorkspaceReadRequest) => {
    return workspace.read(request.projectId, request.path, request.conversationId)
  })

  /** 对话正文里的本机图片：鉴权后签发带随机标识的展示地址，协议按标识回源 */
  ipcMain.handle('workspace:image', (_event, request: WorkspaceReadRequest) => {
    return workspace.image(request.projectId, request.path, request.conversationId)
  })

  // 打开 / 定位本机文件：用户点出来的动作，只做路径归一与安全判定（见 WorkspaceManager.open）
  ipcMain.handle('workspace:open', (_event, request: WorkspaceOpenRequest) => {
    return workspace.open(request.projectId, request.path, !!request.force)
  })

  ipcMain.handle('workspace:reveal', (_event, request: WorkspaceOpenRequest) => {
    return workspace.reveal(request.projectId, request.path)
  })

  /**
   * 拖出窗口：把工作区里的文件 / 文件夹拖到别的应用（资源管理器、聊天窗口、编辑器……）。
   * 浏览器那套拖放给不出本机路径，原生拖放只能由主进程用 webContents.startDrag 发起，
   * 而且要在 dragstart 的当口就发起（界面那边先拦掉默认行为），所以这条用 on 而不是 handle。
   */
  ipcMain.on('workspace:drag', (event, request: WorkspaceDragRequest) => {
    const target = workspace.drag(request?.projectId ?? '', request?.path ?? '')
    // 路径不存在或解析不了：这一拖就是空的，界面上另有「打开 / 在文件夹中显示」两条路
    if (!target.ok) return
    event.sender.startDrag({ file: target.path, icon: dragIcon() })
  })

  ipcMain.handle('workspace:write', (_event, request: WorkspaceWriteRequest) => {
    return workspace.write(request.projectId, request.path, request.content, request.conversationId)
  })

  ipcMain.handle('workspace:mkdir', (_event, request: WorkspaceReadRequest) => {
    return workspace.mkdir(request.projectId, request.path, request.conversationId)
  })

  ipcMain.handle('workspace:rename', (_event, request: WorkspaceRenameRequest) => {
    return workspace.rename(request.projectId, request.path, request.next, request.conversationId)
  })

  /**
   * 粘贴截图：主进程直接读系统剪贴板并保存为临时 PNG。
   * 渲染进程只拿到临时路径，不再把图片转成 base64，也不触发文件服务上传。
   */
  ipcMain.handle('workspace:save-clipboard-image', async () => {
    const items = await clipboard.read()
    let png: Buffer | null = null
    let imageFound = false
    for (const item of items) {
      const imageType = item.types.find((type) => type.startsWith('image/'))
      if (!imageType) continue
      try {
        const payload = await item.getType(imageType)
        if (!(payload instanceof Blob)) continue
        const image = nativeImage.createFromBuffer(Buffer.from(await payload.arrayBuffer()))
        if (!image.isEmpty()) {
          imageFound = true
          png = image.toPNG()
        }
      } catch {
        // 某个剪贴板条目解析失败时继续尝试其它图片 MIME。
      }
      if (png?.length) break
    }
    if (!imageFound) {
      throw new LocalizedError(
        'CLIPBOARD_IMAGE_REQUIRED',
        '剪贴板里没有可保存的图片',
        'The clipboard does not contain an image that can be saved'
      )
    }
    if (!png?.length) {
      throw new LocalizedError(
        'CLIPBOARD_IMAGE_ENCODE_FAILED',
        '截图编码失败',
        'Clipboard image encoding failed'
      )
    }
    const dir = join(runtimeDir, 'userdata', 'temp', 'screenshots')
    mkdirSync(dir, { recursive: true })
    const stamp = new Date().toISOString().replace(/\D/g, '').slice(0, 14)
    const path = join(dir, `screenshot-${stamp}-${randomUUID().slice(0, 8)}.png`)
    writeFileSync(path, png)
    return path
  })

  /**
   * 上传本机文件到文件服务（图床）：主进程读文件内容并按 multipart 直传服务端，
   * 拿回文件标识后即可交给服务端的工具方法（如 multimodalAnalyze）按 id 引用。
   */
  ipcMain.handle('workspace:upload', (event, request: WorkspaceUploadRequest) => {
    return withClientUpgradeNotification(event, () =>
      workspace.upload(
        request.projectId,
        request.path,
        { url: request.url, token: request.token, client: currentClientMeta() },
        request.conversationId
      )
    )
  })

  ipcMain.handle('workspace:remove', (_event, request: WorkspaceReadRequest) => {
    return workspace.remove(request.projectId, request.path, request.conversationId)
  })

  /** 工作区文件清单与文本检索：只覆盖项目已声明的目录（检索由文件工具的 search 动作使用） */
  ipcMain.handle('workspace:files', (_event, request: WorkspaceProjectRequest) => {
    return workspace.files(request.projectId)
  })

  ipcMain.handle('workspace:search', (_event, request: WorkspaceSearchRequest) => {
    return workspace.search(request.projectId, request.keyword)
  })

  /** 文本替换：在指定行范围内把 find 换成新文（范围、唯一性与编码口径都在主进程判定） */
  ipcMain.handle('workspace:replace', (_event, request: WorkspaceReplaceRequest) => {
    return workspace.replace(
      request.projectId,
      request.path,
      request.find,
      request.replace,
      { startLine: request.startLine, endLine: request.endLine, all: request.all },
      request.conversationId
    )
  })

  /** 给模型的本机说明：运行环境 + 工作区目录、主目录、权限与访问策略，追加在项目系统提示词之后 */
  ipcMain.handle('workspace:prompt', (_event, request: WorkspaceProjectRequest) => {
    return workspace.describe(request.projectId)
  })

  /** 本地命令：工作目录按项目声明的目录权限判定，越界时按访问策略处理（申请里带上命令原文） */
  ipcMain.handle('workspace:exec', (_event, request: WorkspaceCommandRequest) => {
    return workspace.exec(
      request.projectId,
      request.command,
      request.cwd,
      request.timeout,
      request.conversationId
    )
  })

  // 本地技能目录：服务端下载 + 解压，渲染进程只发起与展示
  ipcMain.handle('skill:path', () => {
    return skills.path
  })

  ipcMain.handle('skill:list', () => {
    return skills.list()
  })

  ipcMain.handle('skill:install', (event, request: SkillInstallRequest) => {
    return withClientUpgradeNotification(event, () =>
      skills.install({ ...request, client: currentClientMeta() })
    )
  })

  ipcMain.handle('skill:remove', (_event, request: { name: string }) => {
    skills.remove(request.name)
    return { success: true }
  })

  /** @ 引用技能：取技能目录下的 SKILL.md 作为上下文交给模型 */
  ipcMain.handle('skill:read', (_event, request: { name: string }) => {
    return skills.read(request.name)
  })

  /** 技能包文件清单：模型按它挑要读的说明、脚本与参考文档 */
  ipcMain.handle('skill:files', (_event, request: { name: string }) => {
    return skills.files(request.name)
  })

  /** 读技能包内的文件：path 省略时读技能说明 SKILL.md（相对路径由主进程限制在技能目录内） */
  ipcMain.handle('skill:file', (_event, request: SkillFileRequest) => {
    return skills.readFile(request.name, request.path ?? '')
  })

  // 项目配置文件的导入 / 导出：主进程弹原生对话框并读写文件，渲染进程只给内容或拿回原文
  ipcMain.handle('project:export', (_event, request: ProjectExportRequest) => {
    return exportProject(mainWindow, request)
  })

  ipcMain.handle('project:import', () => {
    return importProject(mainWindow)
  })

  // 本地图标：按文件名从图标目录取文件（见 icons.ts），路径由主进程拼
  protocol.handle(ICON_SCHEME, (request) => serveIcon(request.url))
  // 对话正文里的本机图片：地址只带随机标识，文件映射在 images.ts 里（不把路径交给渲染进程）
  protocol.handle(CHAT_IMAGE_SCHEME, (request) => serveChatImage(request.url))

  /** 选择本地图标：主进程复制到图标目录后返回地址，渲染进程只负责回填 */
  ipcMain.handle('icon:pick', () => pickIcon(mainWindow))

  createWindow()

  // 休眠期间撤掉定时器；恢复后按当前时间重算，不补跑休眠期间错过的任务
  powerMonitor.on('suspend', () => schedule.suspend())
  powerMonitor.on('resume', () => schedule.resume())

  app.on('activate', function () {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('before-quit', (event) => {
  if (desktopLifecycleBlocked()) {
    event.preventDefault()
    console.warn('[DesktopAutomation] Blocked automatic application quit during desktop operation')
    return
  }
  if (quitHandshakeDone) return
  event.preventDefault()
  requestQuitHandshake()
})

app.on('will-quit', () => {
  globalShortcut.unregisterAll()
  desktopAutomation.cancel('')
  desktopOverlay.destroy()
  schedule.stop()
  void browserAutomation.closeAll()
  // 退出前把待更新的完整性哈希落盘，并关闭数据库（触发 WAL checkpoint）
  db.close()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.
