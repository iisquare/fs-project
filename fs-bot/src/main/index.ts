import {
  app,
  shell,
  BrowserWindow,
  clipboard,
  dialog,
  ipcMain,
  nativeImage,
  powerMonitor,
  protocol,
  screen,
  type NativeImage
} from 'electron'
import { join } from 'path'
import { mkdirSync, writeFileSync } from 'fs'
import { randomUUID } from 'crypto'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
import { DatabaseManager } from './database/manager'
import { UserSchemaTooNewError } from './database/migrations'
import { ConfigStore } from './config'
import { SkillManager } from './skills'
import { exportProject, importProject } from './projects'
import { ICON_SCHEME, initIcons, pickIcon, serveIcon } from './icons'
import { CHAT_IMAGE_SCHEME, serveChatImage } from './images'
import { parseProjectEnvVars } from './project-env'
import { ScheduleManager } from './schedule'
import { getAutoStartStatus, setAutoStart } from './autostart'
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

let mainWindow: BrowserWindow | null = null

const db = DatabaseManager.getInstance()
const workspace = WorkspaceManager.getInstance()
const skills = SkillManager.getInstance()
const schedule = new ScheduleManager()

/**
 * 等待界面答复的访问申请：id → 答复回调与它所属的会话。
 * 成对清理：界面答复、等到超时、这段会话被停止，三条路都会收尾。
 */
const approvals = new Map<string, { conversationId: string; settle: (allow: boolean) => void }>()
const APPROVAL_TIMEOUT = 5 * 60 * 1000

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
function cancelApprovals(conversationId: string): void {
  const id = String(conversationId ?? '')
  if (!id) return
  for (const [requestId, pending] of [...approvals]) {
    if (pending.conversationId !== id) continue
    if (!closeApproval(requestId)) continue
    pending.settle(false)
  }
}

/**
 * 工作区外的访问申请：推给界面问一次并等答复。
 * 窗口不在（或已销毁）直接按拒绝处理，超时同样按拒绝处理，不让一轮对话悬着。
 */
function requestApproval(request: WorkspaceApproval): Promise<boolean> {
  if (!mainWindow || mainWindow.isDestroyed()) return Promise.resolve(false)
  return new Promise<boolean>((settle) => {
    const timer = setTimeout(() => {
      // 等到超时按拒绝处理：顺手告诉界面这条申请作废了，别让横幅与会话列表上的点一直挂着
      if (closeApproval(request.id)) settle(false)
    }, APPROVAL_TIMEOUT)
    approvals.set(request.id, {
      conversationId: String(request.conversationId ?? ''),
      settle: (allow: boolean) => {
        clearTimeout(timer)
        settle(allow)
      }
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

function createWindow(): void {
  // Create the browser window.
  mainWindow = new BrowserWindow({
    width: 900,
    height: 670,
    show: false,
    frame: false,
    autoHideMenuBar: true,
    ...(process.platform === 'linux' ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
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

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
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
    mainWindow = null
    // 窗口关闭即视为客户端停止：不在后台继续跑定时任务
    schedule.stop()
  })
}

ipcMain.handle('window:set-login-size', () => {
  if (mainWindow) {
    mainWindow.setSize(720, 640)
    mainWindow.center()
    mainWindow.setResizable(false)
  }
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
  if (!mainWindow || pinned || mainWindow.isMaximized()) return

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
 * 主界面窗口的基准尺寸。登录回来后文件区若还开着，以这个尺寸重新加宽，
 * 别把登录页那次固定尺寸算成文件区的占位。
 */
function resetMainWindowSize(): void {
  if (!mainWindow || mainWindow.isDestroyed()) return
  mainWindow.setSize(900, 670)
  mainWindow.center()
  mainWindow.setResizable(true)
  filesPanelApplied = 0
  filesPanelShiftX = 0
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

/** 右侧文件区展开 / 收起 / 拖宽：界面只报意愿与宽度，窗口怎么让位由主进程算 */
ipcMain.handle('window:files-panel', (_event, request: { visible?: boolean; width?: number }) => {
  filesPanelVisible = true === request?.visible
  const width = Number(request?.width)
  if (Number.isFinite(width) && width > 0) filesPanelWidth = Math.round(width)
  applyFilesPanelWidth()
})

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
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
  electronApp.setAppUserModelId('com.electron')

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
    const settings = config.getSettings()
    return {
      clientVersion: app.getVersion(),
      platform: process.platform,
      architecture: process.arch,
      clientId: String(settings.clientId ?? ''),
      channel: String(settings.updateChannel ?? 'stable')
    }
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
      throw new Error('开机启动由系统登录项管理，请使用系统设置页的开关')
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
    if (!row) throw new Error(`项目不存在或已删除：${projectId}`)
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
    // 勾选「始终允许」：把策略切成「帮我批准」，后续工作区外的访问不再逐个询问
    if (request?.allow && request?.always) {
      const policy = workspace.setPolicy('auto')
      config.updateSetting('workspacePolicy', policy)
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
    cancelApprovals(conversationId)
    return { stopped: workspace.cancel(conversationId) }
  })

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
  ipcMain.handle('workspace:save-clipboard-image', () => {
    const image = clipboard.readImage()
    if (image.isEmpty()) throw new Error('剪贴板里没有可保存的图片')
    const png = image.toPNG()
    if (!png.length) throw new Error('截图编码失败')
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
  ipcMain.handle('workspace:upload', (_event, request: WorkspaceUploadRequest) => {
    return workspace.upload(
      request.projectId,
      request.path,
      { url: request.url, token: request.token },
      request.conversationId
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

  ipcMain.handle('skill:install', (_event, request: SkillInstallRequest) => {
    return skills.install(request)
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
app.on('will-quit', () => {
  schedule.stop()
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
