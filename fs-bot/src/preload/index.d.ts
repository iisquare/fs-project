import { ElectronAPI } from '@electron-toolkit/preload'

/** 项目工作区目录权限：只读 / 只写 / 读写 */
export type WorkspacePermission = 'read' | 'write' | 'readwrite'

/** 工作区外文件的访问策略：请求批准 / 帮我批准 / 自动拒绝 / 完全访问权限 */
export type WorkspacePolicy = 'ask' | 'auto' | 'deny' | 'full'

/** 一次本地访问想做什么：读文件 / 写文件 / 执行命令 */
export type WorkspaceNeed = 'read' | 'write' | 'command'

export interface WorkspaceEntry {
  name: string
  path: string
  dir: boolean
  size: number
  updatedAt: number
  ext: string
  permission: WorkspacePermission | null
}

/** 工作区文件清单项：root 是命中的工作区目录，供界面展示相对路径 */
export interface WorkspaceFile {
  name: string
  path: string
  root: string
  ext: string
  dir: boolean
}

export interface WorkspaceMatch {
  path: string
  /** 命中的起始行（从 1 开始） */
  line: number
  /** 命中的结束行：单行命中与 line 相同，多行命中给出一段范围（展示成 `12-15`） */
  endLine: number
  text: string
}

/** 一次文本替换的回执：替换了几处、按哪种口径命中、行尾与 BOM 有没有被动过 */
export interface WorkspaceReplaceResult {
  path: string
  count: number
  /** literal = find 整段按字面量命中；segments = 按空白切段、各段按原顺序命中 */
  match: 'literal' | 'segments'
  startLine: number
  endLine: number
  /** 被替换掉的原文（折叠空白后截断，多处时是头一处） */
  before: string
  /** 换上去的新文（折叠空白后截断） */
  after: string
  eol: 'lf' | 'crlf'
  bom: boolean
  size: number
}

/** 引用一段本地路径的结果：文件给内容、目录给清单 */
export interface WorkspaceReference {
  path: string
  dir: boolean
  size: number
  text: string
  truncated: boolean
}

/** 上传本机文件到文件服务（图床）的回执：文件标识与文件信息，随后按 id 引用该文件 */
export interface WorkspaceFileUpload {
  path: string
  id: string
  name: string
  type: string
  suffix: string
  size: number
}

/** 打开 / 定位本机文件的结果：ok 为 false 时 message 是不能照做的原因 */
export interface WorkspaceOpenResult {
  ok: boolean
  /** 归一后的路径（定位失败时是调用方给的那一段） */
  path: string
  /** 拒绝的原因是「可执行 / 脚本文件」：界面确认后带 force 再来一次 */
  unsafe?: boolean
  message?: string
}

/** 工作区外的访问申请：界面弹窗后按 id 答复 */
export interface WorkspaceApproval {
  id: string
  projectId: string
  path: string
  need: WorkspaceNeed
  /** 执行命令时带上的命令原文 */
  command?: string
  /** 发起这次访问的会话：界面据此把申请交回那段对话 */
  conversationId?: string
}

/** 本地命令的执行结果：退出码为 null 表示被信号终止或没能启动 */
export interface WorkspaceCommandResult {
  command: string
  cwd: string
  code: number | null
  signal: string
  timedOut: boolean
  /** 用户按了停止：进程树已被收掉，这不是命令自己的结束 */
  cancelled: boolean
  duration: number
  stdout: string
  stderr: string
}

export interface InstalledSkill {
  name: string
  path: string
  fileCount: number
  size: number
  meta: {
    skillId: number
    skillName: string
    versionId: number
    versionName: string
    installedAt: number
  } | null
  /** 技能摘要：SKILL.md 里 YAML 的 description，没有就取第一段正文 */
  summary: string
}

/** 技能包内的一个文件：相对技能目录的路径（用 / 分隔）与大小 */
export interface SkillFileEntry {
  path: string
  size: number
}

declare global {
  interface Window {
    electron: ElectronAPI
    ipc: {
      /** 拖入的文件 / 文件夹的绝对路径：取不到路径的项（不是磁盘上的文件）已过滤掉 */
      filePaths: (files: File[]) => string[]
      setLoginSize: () => Promise<void>
      setNormalSize: () => Promise<void>
      minimize: () => Promise<void>
      maximize: () => Promise<void>
      close: () => Promise<void>
      isMaximized: () => Promise<boolean>
      onMaximizeChange: (callback: (isMaximized: boolean) => void) => void
      togglePin: (pin: boolean) => Promise<void>
      db: {
        initUser: (
          serial: string,
          userId: string,
          dbSecret: string
        ) => Promise<{ success: boolean }>
        select: (
          table: string,
          where?: Record<string, unknown>,
          orderBy?: string
        ) => Promise<{ code: number; message: string; data: unknown[] }>
        insert: (
          table: string,
          data: Record<string, unknown>
        ) => Promise<{ code: number; message: string; data: unknown }>
        update: (
          table: string,
          where: Record<string, unknown>,
          data: Record<string, unknown>
        ) => Promise<{ code: number; message: string; data: null }>
        upsert: (
          table: string,
          data: Record<string, unknown>
        ) => Promise<{ code: number; message: string; data: unknown }>
        delete: (
          table: string,
          where: Record<string, unknown>
        ) => Promise<{ code: number; message: string; data: null }>
        /** 关键词检索：columns 逐列按 LIKE 匹配，另有等值（where）与集合（in）条件 */
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
        ) => Promise<{ code: number; message: string; data: unknown[] }>
      }
      config: {
        getSettings: () => Promise<Record<string, string | number | boolean>>
        updateSetting: (
          key: string,
          value: string | number | boolean
        ) => Promise<{ success: boolean }>
        listLoginHistory: () => Promise<string[]>
        saveLoginHistory: (serial: string) => Promise<string[]>
        removeLoginHistory: (serial: string) => Promise<string[]>
      }
      workspace: {
        pick: () => Promise<string>
        pickPaths: (kind: 'file' | 'directory') => Promise<string[]>
        list: (
          projectId: string,
          path: string,
          conversationId?: string
        ) => Promise<WorkspaceEntry[]>
        read: (
          projectId: string,
          path: string,
          conversationId?: string
        ) => Promise<{ path: string; content: string; size: number }>
        reference: (
          projectId: string,
          path: string,
          conversationId?: string
        ) => Promise<WorkspaceReference>
        /** 用系统默认程序打开本机文件 / 目录（可执行文件先由主进程拒绝，确认后带 force 重试） */
        open: (projectId: string, path: string, force?: boolean) => Promise<WorkspaceOpenResult>
        /** 把本机文件 / 目录拖出窗口：在 dragstart 里调，真正的原生拖放由主进程发起 */
        startDrag: (projectId: string, path: string) => void
        /** 在系统文件管理器中定位这个文件 / 目录 */
        reveal: (projectId: string, path: string) => Promise<WorkspaceOpenResult>
        write: (
          projectId: string,
          path: string,
          content: string,
          conversationId?: string
        ) => Promise<{ path: string; size: number }>
        mkdir: (
          projectId: string,
          path: string,
          conversationId?: string
        ) => Promise<{ path: string }>
        rename: (
          projectId: string,
          path: string,
          next: string,
          conversationId?: string
        ) => Promise<{ path: string }>
        upload: (
          projectId: string,
          path: string,
          target: { url: string; token: string },
          conversationId?: string
        ) => Promise<WorkspaceFileUpload>
        remove: (
          projectId: string,
          path: string,
          conversationId?: string
        ) => Promise<{ path: string }>
        files: (projectId: string) => Promise<WorkspaceFile[]>
        search: (projectId: string, keyword: string) => Promise<WorkspaceMatch[]>
        /** 文本替换：在指定行范围内把 find 换成新文（find 与替换内容都支持多行与特殊字符） */
        replace: (
          projectId: string,
          path: string,
          find: string,
          replace: string,
          scope?: { startLine?: number; endLine?: number; all?: boolean },
          conversationId?: string
        ) => Promise<WorkspaceReplaceResult>
        /** 给模型的本机说明：运行环境 + 工作区目录、主目录、权限与访问策略（追加在系统提示词之后） */
        prompt: (projectId: string) => Promise<string>
        exec: (
          projectId: string,
          command: string,
          cwd?: string,
          timeout?: number,
          conversationId?: string
        ) => Promise<WorkspaceCommandResult>
        /** 停止生成：收掉这段会话还挂着的本地活儿（等答复的申请按拒绝处理、在跑的命令终止） */
        cancel: (conversationId: string) => Promise<{ stopped: number }>
        policy: () => Promise<WorkspacePolicy>
        setPolicy: (policy: WorkspacePolicy) => Promise<WorkspacePolicy>
        approve: (request: {
          id: string
          allow: boolean
          always?: boolean
        }) => Promise<{ success: boolean }>
        onApprovalRequest: (callback: (request: WorkspaceApproval) => void) => () => void
        /** 申请作废（主进程等到超时、按拒绝处理）：界面据此撤掉横幅 */
        onApprovalClosed: (callback: (request: { id: string }) => void) => () => void
      }
      icon: {
        /** 选图后返回可直接放进 img 的地址（取消返回空串） */
        pick: () => Promise<string>
      }
      skill: {
        path: () => Promise<string>
        list: () => Promise<InstalledSkill[]>
        install: (request: {
          baseUrl: string
          token: string
          skillId: number
          skillName: string
          versionId: number
          versionName: string
        }) => Promise<InstalledSkill>
        remove: (name: string) => Promise<{ success: boolean }>
        read: (name: string) => Promise<{ name: string; path: string; content: string }>
        /** 技能包内的文件清单（不含目录），相对技能目录的路径用 / 分隔 */
        files: (name: string) => Promise<{
          name: string
          path: string
          files: SkillFileEntry[]
        }>
        /** 读技能包内的文件：path 省略时读 SKILL.md，超长按上限截断 */
        file: (
          name: string,
          path?: string
        ) => Promise<{
          name: string
          path: string
          size: number
          content: string
          truncated: boolean
        }>
      }
      project: {
        /** 导出项目配置：弹保存对话框写入文件，取消时 path 为空串 */
        exportFile: (filename: string, content: string) => Promise<{ path: string }>
        /** 导入项目配置：弹选择对话框读回原文，取消时 path 与 content 都是空串 */
        importFile: () => Promise<{ path: string; content: string }>
      }
    }
  }
}
