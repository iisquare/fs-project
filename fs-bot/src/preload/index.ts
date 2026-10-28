import { contextBridge, ipcRenderer, webUtils } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'

const ipc = {
  // 拖进对话区的文件 / 文件夹：File 对象只在渲染进程手里，路径只能由 preload 用 webUtils 取
  // （Electron 32 起 File.path 已移除）。回传的是纯字符串，File 本身不过桥；
  // 取不到路径的项（不是磁盘上的文件）直接丢掉，不让一条坏项毁掉整次拖入。
  filePaths: (files: File[]) =>
    (files ?? [])
      .map((file) => {
        try {
          return webUtils.getPathForFile(file)
        } catch {
          return ''
        }
      })
      .filter((path) => !!path),
  setLoginSize: () => ipcRenderer.invoke('window:set-login-size'),
  setNormalSize: () => ipcRenderer.invoke('window:set-normal-size'),
  minimize: () => ipcRenderer.invoke('window:minimize'),
  maximize: () => ipcRenderer.invoke('window:maximize'),
  close: () => ipcRenderer.invoke('window:close'),
  quit: () => ipcRenderer.invoke('app:quit'),
  isMaximized: () => ipcRenderer.invoke('window:is-maximized'),
  appVersions: () => ipcRenderer.invoke('app:versions'),
  clientMeta: () => ipcRenderer.invoke('app:client-meta'),
  update: {
    downloadAndInstall: (request: {
      url: string
      token: string
      fileName: string
      fileSize: number
      sha512: string
      version: string
      currentVersion: string
      platform: string
      architecture: string
      delta?: {
        blockSize: number
        blocks: Array<{ checksum: string; size: number }>
      } | null
    }) => ipcRenderer.invoke('app:update:download', request),
    status: () => ipcRenderer.invoke('app:update:status'),
    controlStatus: () => ipcRenderer.invoke('app:update:control'),
    pause: () => ipcRenderer.invoke('app:update:pause'),
    cancel: () => ipcRenderer.invoke('app:update:cancel'),
    reportHealthy: () => ipcRenderer.invoke('app:update:healthy'),
    onProgress: (
      callback: (progress: { received: number; total: number; percent: number }) => void
    ) => {
      const listener = (
        _: unknown,
        progress: { received: number; total: number; percent: number }
      ): void => callback(progress)
      ipcRenderer.on('app:update:progress', listener)
      return () => ipcRenderer.removeListener('app:update:progress', listener)
    }
  },
  autoStart: {
    get: () => ipcRenderer.invoke('app:auto-start:get'),
    set: (enabled: boolean) => ipcRenderer.invoke('app:auto-start:set', { enabled })
  },
  onMaximizeChange: (callback: (isMaximized: boolean) => void) => {
    ipcRenderer.on('window:maximize-change', (_, isMaximized) => callback(isMaximized))
  },
  togglePin: (pin: boolean) => ipcRenderer.invoke('window:toggle-pin', pin),
  // 右侧文件区的展开状态与宽度：主进程据此把窗口等量加宽 / 收窄
  setFilesPanel: (visible: boolean, width: number) =>
    ipcRenderer.invoke('window:files-panel', { visible, width }),
  db: {
    initUser: (serial: string, userId: string, dbSecret: string) =>
      ipcRenderer.invoke('db:init-user', { serial, userId, dbSecret }),
    select: (table: string, where?: Record<string, unknown>, orderBy?: string, limit?: number) =>
      ipcRenderer.invoke('db:select', { table, where, orderBy, limit }),
    insert: (table: string, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:insert', { table, data }),
    update: (table: string, where: Record<string, unknown>, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:update', { table, where, data }),
    upsert: (table: string, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:upsert', { table, data }),
    delete: (table: string, where: Record<string, unknown>) =>
      ipcRenderer.invoke('db:delete', { table, where }),
    deleteBatch: (
      deletes: Array<{
        table: string
        where?: Record<string, unknown>
        in?: Record<string, unknown[]>
      }>
    ) => ipcRenderer.invoke('db:delete-batch', { deletes }),
    // 关键词检索：历史对话（记忆）按标题与消息正文找行，比把整表拉回来自己过滤省事；
    // mode 给 segments 时按空白切成多段匹配（多行片段的兜底口径，见 ConversationDb.search）
    search: (
      table: string,
      columns: string[],
      keyword: string,
      options?: {
        where?: Record<string, unknown>
        in?: Record<string, unknown[]>
        orderBy?: string
        limit?: number
        mode?: 'literal' | 'segments'
      }
    ) => ipcRenderer.invoke('db:search', { table, columns, keyword, ...(options ?? {}) })
  },
  config: {
    getSettings: () => ipcRenderer.invoke('config:get-settings'),
    updateSetting: (key: string, value: string | number | boolean) =>
      ipcRenderer.invoke('config:update-setting', { key, value }),
    listLoginHistory: () => ipcRenderer.invoke('config:login-history:list'),
    saveLoginHistory: (serial: string) =>
      ipcRenderer.invoke('config:login-history:save', { serial }),
    removeLoginHistory: (serial: string) =>
      ipcRenderer.invoke('config:login-history:remove', { serial })
  },
  // 定时调度：主进程只挂一个定时器，渲染进程只同步任务快照并接收到点通知
  schedule: {
    sync: (tasks: Array<{ id: string; cron: string; enabled: boolean }>) =>
      ipcRenderer.invoke('schedule:sync', tasks),
    stop: () => ipcRenderer.invoke('schedule:stop'),
    onTrigger: (callback: (ids: string[]) => void) => {
      const listener = (_: unknown, ids: string[]): void => callback(ids)
      ipcRenderer.on('schedule:trigger', listener)
      return () => ipcRenderer.removeListener('schedule:trigger', listener)
    }
  },
  workspace: {
    // 选一个本机路径：kind 决定选文件还是目录，title 是对话框标题
    pick: (kind: 'file' | 'directory' = 'directory', title = '') =>
      ipcRenderer.invoke('workspace:pick', { kind, title }),
    // 引用本地文件 / 文件夹：选择时不限制路径，权限在读写时才校验
    pickPaths: (kind: 'file' | 'directory') => ipcRenderer.invoke('workspace:pick-paths', { kind }),
    // 文件操作都带上项目标识与会话标识：项目决定权限口径，会话决定审批申请交回哪段对话
    list: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:list', { projectId, path, conversationId }),
    read: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:read', { projectId, path, conversationId }),
    // 对话正文里的本机图片：鉴权后拿展示地址（地址由主进程映射，渲染进程拿不到绝对路径）
    image: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:image', { projectId, path, conversationId }),
    // 引用一段本地路径：文件给内容、目录给清单，调用方不区分两者
    reference: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:reference', { projectId, path, conversationId }),
    // 用系统默认程序打开 / 在文件管理器里定位：用户点出来的动作，工作区外的路径也照开，
    // 只有可执行 / 脚本类文件默认拒绝（force 是界面确认过之后的重试）
    open: (projectId: string, path: string, force = false) =>
      ipcRenderer.invoke('workspace:open', { projectId, path, force }),
    reveal: (projectId: string, path: string) =>
      ipcRenderer.invoke('workspace:reveal', { projectId, path }),
    /**
     * 把工作区里的文件 / 文件夹拖出窗口（拖到资源管理器、聊天窗口、编辑器……）：
     * 浏览器那套拖放给不出本机路径，原生拖放得请主进程用 webContents.startDrag 发起
     * （见主进程的 workspace:drag），所以这里是 send——界面在 dragstart 里先拦掉默认行为，
     * 主进程在同一时机接手。
     */
    startDrag: (projectId: string, path: string) =>
      ipcRenderer.send('workspace:drag', { projectId, path }),
    write: (projectId: string, path: string, content: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:write', { projectId, path, content, conversationId }),
    mkdir: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:mkdir', { projectId, path, conversationId }),
    rename: (projectId: string, path: string, next: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:rename', { projectId, path, next, conversationId }),
    // 粘贴截图：主进程读系统剪贴板并保存为临时 PNG，回传路径供输入框写成 @ 引用
    saveClipboardImage: () => ipcRenderer.invoke('workspace:save-clipboard-image'),
    // 上传本机文件到文件服务（图床）：拿回文件标识后即可交给服务端的工具方法按 id 引用
    upload: (
      projectId: string,
      path: string,
      target: { url: string; token: string },
      conversationId = ''
    ) =>
      ipcRenderer.invoke('workspace:upload', {
        projectId,
        path,
        url: target?.url ?? '',
        token: target?.token ?? '',
        conversationId
      }),
    remove: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:remove', { projectId, path, conversationId }),
    // 清单与检索：只覆盖项目已声明的目录（检索由文件工具的 search 动作使用）
    files: (projectId: string) => ipcRenderer.invoke('workspace:files', { projectId }),
    search: (projectId: string, keyword: string) =>
      ipcRenderer.invoke('workspace:search', { projectId, keyword }),
    // 文本替换：在指定行范围内把 find 换成新文（行尾与编码保真、唯一性判定都在主进程）
    replace: (
      projectId: string,
      path: string,
      find: string,
      replace: string,
      scope?: { startLine?: number; endLine?: number; all?: boolean },
      conversationId = ''
    ) =>
      ipcRenderer.invoke('workspace:replace', {
        projectId,
        path,
        find,
        replace,
        ...(scope ?? {}),
        conversationId
      }),
    // 给模型的本机说明：运行环境 + 工作区目录、主目录、权限与访问策略，追加在项目系统提示词之后
    prompt: (projectId: string) => ipcRenderer.invoke('workspace:prompt', { projectId }),
    // 本机执行命令：工作目录决定权限口径，越界时主进程会把申请推上来等答复
    exec: (projectId: string, command: string, cwd = '', timeout = 0, conversationId = '') =>
      ipcRenderer.invoke('workspace:exec', { projectId, command, cwd, timeout, conversationId }),
    // 停止生成：收掉这段会话还挂着的本地活儿（等答复的申请按拒绝处理、在跑的命令终止）
    cancel: (conversationId: string) => ipcRenderer.invoke('workspace:cancel', { conversationId }),
    // 工作区外文件的访问策略：读取 / 切换（策略由主进程执行，渲染进程只发起）
    policy: () => ipcRenderer.invoke('workspace:policy:get'),
    setPolicy: (policy: string) => ipcRenderer.invoke('workspace:policy:set', { policy }),
    // 访问申请的答复：允许本次放行，always 勾选后同步切到「帮我批准」
    approve: (request: { id: string; allow: boolean; always?: boolean }) =>
      ipcRenderer.invoke('workspace:approval:response', request),
    onApprovalRequest: (callback: (request: unknown) => void) => {
      const listener = (_: unknown, request: unknown): void => callback(request)
      ipcRenderer.on('workspace:approval-request', listener)
      return () => ipcRenderer.removeListener('workspace:approval-request', listener)
    },
    // 申请作废（主进程等到超时、按拒绝处理）：界面据此撤掉横幅，不留一条点不动的申请
    onApprovalClosed: (callback: (request: { id: string }) => void) => {
      const listener = (_: unknown, request: { id: string }): void => callback(request)
      ipcRenderer.on('workspace:approval-closed', listener)
      return () => ipcRenderer.removeListener('workspace:approval-closed', listener)
    }
  },
  // 本地图标：由主进程选图并复制到图标目录，渲染进程只回填拿到的地址
  icon: {
    pick: () => ipcRenderer.invoke('icon:pick')
  },
  skill: {
    path: () => ipcRenderer.invoke('skill:path'),
    list: () => ipcRenderer.invoke('skill:list'),
    install: (request: {
      baseUrl: string
      token: string
      skillId: number
      skillName: string
      versionId: number
      versionName: string
    }) => ipcRenderer.invoke('skill:install', request),
    remove: (name: string) => ipcRenderer.invoke('skill:remove', { name }),
    read: (name: string) => ipcRenderer.invoke('skill:read', { name }),
    // 技能包内文件：清单给模型挑，读取按相对路径（省略时读 SKILL.md）
    files: (name: string) => ipcRenderer.invoke('skill:files', { name }),
    file: (name: string, path = '') => ipcRenderer.invoke('skill:file', { name, path })
  },
  // 项目配置文件的导入 / 导出：主进程弹原生对话框选路径，渲染进程只给内容或拿回原文
  project: {
    exportFile: (filename: string, content: string) =>
      ipcRenderer.invoke('project:export', { filename, content }),
    importFile: () => ipcRenderer.invoke('project:import')
  }
}

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('ipc', ipc)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.ipc = ipc
}
