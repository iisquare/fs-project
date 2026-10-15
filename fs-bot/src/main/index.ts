import { app, shell, BrowserWindow, dialog, ipcMain, protocol } from 'electron'
import { join, dirname } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
import { DatabaseManager } from './database/manager'
import { ConfigStore } from './config'
import { SkillManager } from './skills'
import { exportProject, importProject } from './projects'
import { ICON_SCHEME, initIcons, pickIcon, serveIcon } from './icons'
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
  DbSearchRequest,
  ConfigUpdateSettingRequest,
  ConfigLoginHistoryRequest,
  WorkspaceReadRequest,
  WorkspaceWriteRequest,
  WorkspaceRenameRequest,
  WorkspaceProjectRequest,
  WorkspaceSearchRequest,
  WorkspaceCommandRequest,
  WorkspaceUploadRequest,
  WorkspacePolicyRequest,
  WorkspaceApprovalResponse,
  SkillInstallRequest,
  SkillFileRequest,
  ProjectExportRequest
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

/** 等待界面答复的访问申请：id → 答复回调（成对清理，界面答复或超时都会收尾） */
const approvals = new Map<string, (allow: boolean) => void>()
const APPROVAL_TIMEOUT = 5 * 60 * 1000

/**
 * 工作区外的访问申请：推给界面问一次并等答复。
 * 窗口不在（或已销毁）直接按拒绝处理，超时同样按拒绝处理，不让一轮对话悬着。
 */
function requestApproval(request: WorkspaceApproval): Promise<boolean> {
  if (!mainWindow || mainWindow.isDestroyed()) return Promise.resolve(false)
  return new Promise<boolean>((settle) => {
    const timer = setTimeout(() => {
      if (!approvals.delete(request.id)) return
      // 等到超时按拒绝处理：顺手告诉界面这条申请作废了，别让横幅与会话列表上的点一直挂着
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('workspace:approval-closed', { id: request.id })
      }
      settle(false)
    }, APPROVAL_TIMEOUT)
    approvals.set(request.id, (allow: boolean) => {
      clearTimeout(timer)
      settle(allow)
    })
    mainWindow!.webContents.send('workspace:approval-request', request)
  })
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
}

ipcMain.handle('window:set-login-size', () => {
  if (mainWindow) {
    mainWindow.setSize(450, 600)
    mainWindow.center()
    mainWindow.setResizable(false)
  }
})

ipcMain.handle('window:set-normal-size', () => {
  if (mainWindow) {
    mainWindow.setSize(900, 670)
    mainWindow.center()
    mainWindow.setResizable(true)
  }
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

ipcMain.handle('window:is-maximized', () => {
  return mainWindow?.isMaximized() ?? false
})

let savedBounds: { x: number; y: number; width: number; height: number } | null = null

ipcMain.handle('window:toggle-pin', (_event, pin: boolean) => {
  if (!mainWindow) return

  if (pin) {
    const bounds = mainWindow.getBounds()
    savedBounds = { x: bounds.x, y: bounds.y, width: bounds.width, height: bounds.height }
    mainWindow.setAlwaysOnTop(true)
    mainWindow.setSize(400, 600)
    mainWindow.center()
  } else {
    mainWindow.setAlwaysOnTop(false)
    if (savedBounds) {
      mainWindow.setBounds(savedBounds)
      savedBounds = null
    }
  }
})

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(async () => {
  const exePath = app.getPath('exe')
  const exeDir = dirname(exePath)
  const installDir = is.dev
    ? app.getAppPath() // 开发模式：项目根目录即安装目录
    : process.platform === 'darwin'
      ? dirname(dirname(exeDir)) // macOS 打包后：往上两级到 .app 包根
      : exeDir // Windows/Linux 打包后：exe 所在目录即安装目录

  if (is.dev) {
    const c = (code: number, s: string): string => `\x1b[${code}m${s}\x1b[0m`
    const pad = (label: string, value: string): string =>
      `  ${c(36, label.padEnd(14))}: ${c(32, value)}`

    console.log(c(90, '-- runtime info ------------------------------------------'))
    console.log(pad('runtimeDir', installDir))
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
  config.init(installDir)
  // 技能目录初始化：技能包解压到这里，页面按目录名展示已安装技能
  skills.init(installDir)
  // 图标目录初始化：选中的本地图片复制到这里，界面按 app-icon://<文件名> 引用
  initIcons(installDir)

  ipcMain.handle('db:init-user', (_event, request: DbInitUserRequest) => {
    db.initUser(request.serial, request.userId, request.dbSecret, installDir)
    return { success: true }
  })

  ipcMain.handle('db:select', (_event, request: DbSelectRequest) => {
    return db.select(request.table, request.where, request.orderBy)
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

  /** 关键词检索：历史对话（记忆）按标题与消息正文找行，由主进程一条 SQL 查完 */
  ipcMain.handle('db:search', (_event, request: DbSearchRequest) => {
    return db.search(request.table, request.columns, request.keyword, {
      where: request.where,
      in: request.in,
      orderBy: request.orderBy,
      limit: request.limit
    })
  })

  ipcMain.handle('config:get-settings', () => {
    return config.getSettings()
  })

  ipcMain.handle('config:update-setting', (_event, request: ConfigUpdateSettingRequest) => {
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
    const settle = approvals.get(String(request?.id ?? ''))
    // 勾选「始终允许」：把策略切成「帮我批准」，后续工作区外的访问不再逐个询问
    if (request?.allow && request?.always) {
      const policy = workspace.setPolicy('auto')
      config.updateSetting('workspacePolicy', policy)
    }
    if (settle) {
      approvals.delete(String(request?.id ?? ''))
      settle(request?.allow === true)
    }
    return { success: true }
  })

  ipcMain.handle('workspace:pick', async () => {
    if (!mainWindow) return ''
    const result = await dialog.showOpenDialog(mainWindow, {
      title: '选择工作区目录',
      properties: ['openDirectory', 'createDirectory']
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

  /** 工作区文件清单与文本检索：@ 引用与文件工具共用，只覆盖项目已声明的目录 */
  ipcMain.handle('workspace:files', (_event, request: WorkspaceProjectRequest) => {
    return workspace.files(request.projectId)
  })

  ipcMain.handle('workspace:search', (_event, request: WorkspaceSearchRequest) => {
    return workspace.search(request.projectId, request.keyword)
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

  /** 选择本地图标：主进程复制到图标目录后返回地址，渲染进程只负责回填 */
  ipcMain.handle('icon:pick', () => pickIcon(mainWindow))

  createWindow()

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
