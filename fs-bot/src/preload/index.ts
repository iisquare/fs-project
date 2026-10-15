import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'

const ipc = {
  setLoginSize: () => ipcRenderer.invoke('window:set-login-size'),
  setNormalSize: () => ipcRenderer.invoke('window:set-normal-size'),
  minimize: () => ipcRenderer.invoke('window:minimize'),
  maximize: () => ipcRenderer.invoke('window:maximize'),
  close: () => ipcRenderer.invoke('window:close'),
  isMaximized: () => ipcRenderer.invoke('window:is-maximized'),
  onMaximizeChange: (callback: (isMaximized: boolean) => void) => {
    ipcRenderer.on('window:maximize-change', (_, isMaximized) => callback(isMaximized))
  },
  togglePin: (pin: boolean) => ipcRenderer.invoke('window:toggle-pin', pin),
  db: {
    initUser: (serial: string, userId: string, dbSecret: string) =>
      ipcRenderer.invoke('db:init-user', { serial, userId, dbSecret }),
    select: (table: string, where?: Record<string, unknown>, orderBy?: string) =>
      ipcRenderer.invoke('db:select', { table, where, orderBy }),
    insert: (table: string, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:insert', { table, data }),
    update: (table: string, where: Record<string, unknown>, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:update', { table, where, data }),
    upsert: (table: string, data: Record<string, unknown>) =>
      ipcRenderer.invoke('db:upsert', { table, data }),
    delete: (table: string, where: Record<string, unknown>) =>
      ipcRenderer.invoke('db:delete', { table, where }),
    // 关键词检索：历史对话（记忆）按标题与消息正文找行，比把整表拉回来自己过滤省事
    search: (
      table: string,
      columns: string[],
      keyword: string,
      options?: {
        where?: Record<string, unknown>
        in?: Record<string, unknown[]>
        orderBy?: string
        limit?: number
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
  workspace: {
    pick: () => ipcRenderer.invoke('workspace:pick'),
    // 引用本地文件 / 文件夹：选择时不限制路径，权限在读写时才校验
    pickPaths: (kind: 'file' | 'directory') => ipcRenderer.invoke('workspace:pick-paths', { kind }),
    // 文件操作都带上项目标识与会话标识：项目决定权限口径，会话决定审批申请交回哪段对话
    list: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:list', { projectId, path, conversationId }),
    read: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:read', { projectId, path, conversationId }),
    // 引用一段本地路径：文件给内容、目录给清单，调用方不区分两者
    reference: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:reference', { projectId, path, conversationId }),
    write: (projectId: string, path: string, content: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:write', { projectId, path, content, conversationId }),
    mkdir: (projectId: string, path: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:mkdir', { projectId, path, conversationId }),
    rename: (projectId: string, path: string, next: string, conversationId = '') =>
      ipcRenderer.invoke('workspace:rename', { projectId, path, next, conversationId }),
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
    // 清单与检索：@ 引用与文件工具共用，只覆盖项目已声明的目录
    files: (projectId: string) => ipcRenderer.invoke('workspace:files', { projectId }),
    search: (projectId: string, keyword: string) =>
      ipcRenderer.invoke('workspace:search', { projectId, keyword }),
    // 给模型的本机说明：运行环境 + 工作区目录、主目录、权限与访问策略，追加在项目系统提示词之后
    prompt: (projectId: string) => ipcRenderer.invoke('workspace:prompt', { projectId }),
    // 本机执行命令：工作目录决定权限口径，越界时主进程会把申请推上来等答复
    exec: (projectId: string, command: string, cwd = '', timeout = 0, conversationId = '') =>
      ipcRenderer.invoke('workspace:exec', { projectId, command, cwd, timeout, conversationId }),
    // 工作区外文件的访问策略：读取 / 切换（策略由主进程执行，渲染进程只发起）
    policy: () => ipcRenderer.invoke('workspace:policy:get'),
    setPolicy: (policy: string) => ipcRenderer.invoke('workspace:policy:set', { policy }),
    // 访问申请的答复：允许本次放行，always 勾选后同步切到「帮我批准」
    approve: (request: { id: string; allow: boolean; always?: boolean }) =>
      ipcRenderer.invoke('workspace:approval:response', request),
    onApprovalRequest: (callback: (request: any) => void) => {
      const listener = (_: unknown, request: any): void => callback(request)
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
