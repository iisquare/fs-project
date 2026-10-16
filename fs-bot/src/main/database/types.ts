export interface DbSelectRequest {
  table: string
  where?: Record<string, unknown>
  orderBy?: string
}

export interface DbInsertRequest {
  table: string
  data: Record<string, unknown>
}

export interface DbUpdateRequest {
  table: string
  where: Record<string, unknown>
  data: Record<string, unknown>
}

export interface DbUpsertRequest {
  table: string
  data: Record<string, unknown>
}

export interface DbDeleteRequest {
  table: string
  where: Record<string, unknown>
}

/** 关键词检索：columns 里的每一列按 LIKE 匹配，where 为等值条件，in 为集合条件 */
export interface DbSearchRequest {
  table: string
  columns: string[]
  keyword: string
  where?: Record<string, unknown>
  in?: Record<string, unknown[]>
  orderBy?: string
  limit?: number
}

export interface DbInitUserRequest {
  serial: string
  userId: string
  dbSecret: string
}

/** <运行目录>/userdata/config.toml 的读写请求 */
export interface ConfigUpdateSettingRequest {
  key: string
  value: string | number | boolean
}

export interface ConfigLoginHistoryRequest {
  serial: string
}

/**
 * 工作区文件操作：projectId 说明这次操作属于哪个项目（会话按项目创建），权限按该项目的声明算；
 * conversationId 说明是哪段对话发起的——需要审批时用它把申请交回那段对话，各对话互不串门。
 */
export interface WorkspaceReadRequest {
  projectId: string
  path: string
  conversationId?: string
}

/** 打开 / 定位本机文件：路径可以是绝对路径，也可以是工作区目录下的相对路径 */
export interface WorkspaceOpenRequest {
  projectId: string
  path: string
  /** 已确认要打开可执行 / 脚本文件（界面弹过确认），默认不直接打开这类文件 */
  force?: boolean
}

export interface WorkspaceWriteRequest {
  projectId: string
  path: string
  content: string
  conversationId?: string
}

/** 只带项目标识的请求：文件清单范围、给模型的本机说明都由该项目的声明决定 */
export interface WorkspaceProjectRequest {
  projectId: string
}

/** 工作区文本检索：关键词 */
export interface WorkspaceSearchRequest {
  projectId: string
  keyword: string
}

/** 本机命令执行：command 必填，cwd 省略时用项目的主目录（没标主目录就是第一个目录），timeout 为秒（省略取默认值） */
export interface WorkspaceCommandRequest {
  projectId: string
  command: string
  cwd?: string
  timeout?: number
  conversationId?: string
}

export interface WorkspaceRenameRequest {
  projectId: string
  path: string
  next: string
  conversationId?: string
}

/** 上传本机文件到文件服务：url / token 随登录态由渲染进程带上，文件本体由主进程读并直传 */
export interface WorkspaceUploadRequest {
  projectId: string
  path: string
  url: string
  token: string
  conversationId?: string
}

/** 工作区外文件的访问策略：请求批准 / 帮我批准 / 自动拒绝 / 完全访问权限 */
export interface WorkspacePolicyRequest {
  policy: string
}

/** 工作区外访问申请的答复：允许本次放行，或勾选始终允许后把策略切成「帮我批准」 */
export interface WorkspaceApprovalResponse {
  id: string
  allow: boolean
  always?: boolean
}

/** 技能安装：从服务端下载指定版本并解压到本地 skills 目录 */
export interface SkillInstallRequest {
  baseUrl: string
  token: string
  skillId: number
  skillName: string
  versionId: number
  versionName: string
}

/** 读技能包内的文件：path 省略时读技能说明 SKILL.md */
export interface SkillFileRequest {
  name: string
  path?: string
}

/** 导出项目配置：filename 是默认文件名，content 是已经序列化好的文件内容 */
export interface ProjectExportRequest {
  filename: string
  content: string
}

/** 导入项目配置：content 是文件原文（取消选择时两者都是空串） */
export interface ProjectImportResult {
  path: string
  content: string
}
