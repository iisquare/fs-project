import Db from '@renderer/core/Db'
import ApiUtil from '@renderer/utils/ApiUtil'

const TABLE = 'projects'

/** 环境变量：secret 仅表示界面上的掩码显示，值本身存在本机加密库里 */
export interface ProjectEnv {
  key: string
  value: string
  secret: boolean
}

/** 工作区目录：权限决定主进程允不允许读 / 写 */
export interface ProjectWorkspace {
  path: string
  permission: 'read' | 'write' | 'readwrite'
  /**
   * 主目录：命令省略 cwd 时在它里面执行，相对路径优先按它解析。
   * 一份清单里至多标一个；都没标时（旧配置、分享来的配置）按清单顺序取第一个，行为不变。
   */
  primary?: boolean
}

/**
 * 工具类型：与智能体编排里大语言模型节点的工具种类一致
 * - method 工具方法（服务端解析出的 OpenAPI / MCP 方法）
 * - knowledge 知识库、theme 数据主题、ontology 知识图谱本体、agentic 编排应用
 */
export type ProjectToolKind = 'method' | 'knowledge' | 'theme' | 'ontology' | 'agentic'

/**
 * 工具方法的固定参数：只在「固定」时落库，未出现的参数由模型决定。
 *
 * - 标量参数用 `{ auto: false, value }` 固定整体取值（取值按文本下发，对象与数组按 JSON 文本）；
 * - 对象参数用 `fields` 按字段固定，未列出的字段仍由模型决定。
 *
 * 服务端同样按这套绑定办事（`BotService` 把 args 透传给工具实现）：定义里摘掉固定的参数，
 * 调用时用这里的取值补齐，与编排里大语言模型节点的执行变量是同一套实现。
 */
export interface ProjectToolArg {
  /** 是否由模型决定：false 表示按 value 固定下发（对象参数不用它，只认 fields） */
  auto?: boolean
  /** 固定取值（对象与数组按 JSON 文本填写） */
  value?: string
  /** 对象参数按字段固定：字段名 → 绑定 */
  fields?: Record<string, ProjectToolArg>
}

/** 固定参数配置：参数名 → 绑定（只存固定项，空对象表示不固定任何参数） */
export type ProjectToolArgs = Record<string, ProjectToolArg>

/** 项目引用的工具：记录类型与目标，凭据与调用都在服务端 */
export interface ProjectTool {
  kind: ProjectToolKind
  /** 目标标识：工具方法取方法标识，内置能力取对应服务端对象的标识 */
  id: number
  /** 展示名称 */
  name: string
  /** 是否启用：停用的工具不参与项目运行 */
  enabled: boolean
  /** 工具方法专用：所属工具与方法名——按方法调用时用它们定位（列表只展示 name） */
  toolId?: number
  toolName?: string
  methodId?: number
  methodName?: string
  title?: string
  /** 服务端工具的描述：作为给模型的 function 描述下发（取不到时服务端按名称兜底） */
  description?: string
  /** 工具方法专用：固定参数配置（见 ProjectToolArg），未配置的参数由模型决定 */
  args?: ProjectToolArgs
}

export interface Project {
  id: string
  /** 选定的模型名称：模型清单与调用都在服务端 */
  model: string
  /**
   * 手工指定的上下文窗口（词元数）：对话页输入框旁的用量圆环按它算比例。
   * 0 表示不在项目里指定，按服务端在模型清单里声明的窗口（服务端也没声明时窗口未知）。
   */
  contextLength: number
  /** 系统提示词：每轮对话作为首条 system 消息下发，留空则不下发 */
  systemPrompt: string
  /**
   * 长期记忆：用户手工维护的一段文字（「事实 / 偏好 / 已定的决策」这类长期有用的东西），
   * 与系统提示词分开存放；每轮作为提示词的一段追加在系统提示词之后一起下发，留空则不下发。
   * 模型只能读（随请求下发），写入只发生在项目编辑器里，没有工具通道。
   */
  memory: string
  /** 思考强度：low | medium | high | max，留空则由模型与网关取默认值 */
  thinkEffort: string
  name: string
  description: string
  icon: string
  envVars: ProjectEnv[]
  tools: ProjectTool[]
  workspace: ProjectWorkspace[]
  inputs: Record<string, any>
  sort: number
  /** 分享去向：服务端分享项目标识，0 表示未分享 */
  shareId: number
  /** 分享项目的名称：卡片上回显用，服务端改名后以再次发布的结果为准 */
  shareName: string
  /** 最近一次发布的版本号：卡片上回显「已分享 v1.0」 */
  shareVersion: string
  createdAt: string
  updatedAt: string
}

function parse<T>(text: unknown, fallback: T): T {
  if (typeof text !== 'string' || !text) return fallback
  try {
    return JSON.parse(text) as T
  } catch {
    return fallback
  }
}

/** 库内是 snake_case 列，统一在这里转成领域对象 */
function format(row: Record<string, any>): Project {
  return {
    id: String(row['id'] ?? ''),
    model: String(row['model'] ?? ''),
    contextLength: Math.max(0, Math.floor(Number(row['context_length'] ?? 0)) || 0),
    systemPrompt: String(row['system_prompt'] ?? ''),
    memory: String(row['memory'] ?? ''),
    thinkEffort: String(row['think_effort'] ?? ''),
    name: String(row['name'] ?? ''),
    description: String(row['description'] ?? ''),
    icon: String(row['icon'] ?? ''),
    envVars: parse<ProjectEnv[]>(row['env_vars'], []),
    // 缺 enabled 的行按启用处理：开关是后加的，读出来要给个确定的初始状态
    tools: parse<ProjectTool[]>(row['tools'], []).map((tool) => ({
      ...tool,
      enabled: false !== tool?.enabled
    })),
    workspace: parse<ProjectWorkspace[]>(row['workspace'], []),
    inputs: parse<Record<string, any>>(row['inputs'], {}),
    sort: Number(row['sort'] ?? 0),
    shareId: Math.max(0, Math.floor(Number(row['share_id'] ?? 0)) || 0),
    shareName: String(row['share_name'] ?? ''),
    shareVersion: String(row['share_version'] ?? ''),
    createdAt: String(row['created_at'] ?? ''),
    updatedAt: String(row['updated_at'] ?? '')
  }
}

export default {
  async list(): Promise<{ code: number; message: string; data: Project[] }> {
    const result: any = await Db.select(TABLE, undefined, 'sort DESC, updated_at DESC')
    return ApiUtil.result(
      ApiUtil.code(result) ?? 0,
      ApiUtil.message(result),
      (ApiUtil.data(result) as any[]).map(format)
    )
  },

  async create(data: Partial<Project>) {
    const now = new Date().toISOString()
    return Db.insert(TABLE, {
      id: crypto.randomUUID(),
      model: data.model ?? '',
      context_length: Math.max(0, Math.floor(Number(data.contextLength ?? 0)) || 0),
      system_prompt: data.systemPrompt ?? '',
      memory: data.memory ?? '',
      think_effort: data.thinkEffort ?? '',
      name: data.name ?? '未命名项目',
      description: data.description ?? '',
      icon: data.icon ?? '',
      env_vars: JSON.stringify(data.envVars ?? []),
      tools: JSON.stringify(data.tools ?? []),
      workspace: JSON.stringify(data.workspace ?? []),
      inputs: JSON.stringify(data.inputs ?? {}),
      sort: Number(data.sort ?? 0),
      share_id: Math.max(0, Math.floor(Number(data.shareId ?? 0)) || 0),
      share_name: data.shareName ?? '',
      share_version: data.shareVersion ?? '',
      created_at: now,
      updated_at: now
    })
  },

  async update(id: string, data: Partial<Project>) {
    const row: Record<string, unknown> = { updated_at: new Date().toISOString() }
    if (data.model !== undefined) row['model'] = data.model
    if (data.contextLength !== undefined) {
      row['context_length'] = Math.max(0, Math.floor(Number(data.contextLength)) || 0)
    }
    if (data.systemPrompt !== undefined) row['system_prompt'] = data.systemPrompt
    if (data.memory !== undefined) row['memory'] = data.memory
    if (data.thinkEffort !== undefined) row['think_effort'] = data.thinkEffort
    if (data.name !== undefined) row['name'] = data.name
    if (data.description !== undefined) row['description'] = data.description
    if (data.icon !== undefined) row['icon'] = data.icon
    if (data.envVars !== undefined) row['env_vars'] = JSON.stringify(data.envVars)
    if (data.tools !== undefined) row['tools'] = JSON.stringify(data.tools)
    if (data.workspace !== undefined) row['workspace'] = JSON.stringify(data.workspace)
    if (data.inputs !== undefined) row['inputs'] = JSON.stringify(data.inputs)
    if (data.sort !== undefined) row['sort'] = Number(data.sort)
    if (data.shareId !== undefined) {
      row['share_id'] = Math.max(0, Math.floor(Number(data.shareId)) || 0)
    }
    if (data.shareName !== undefined) row['share_name'] = data.shareName
    if (data.shareVersion !== undefined) row['share_version'] = data.shareVersion
    return Db.update(TABLE, { id }, row)
  },

  async delete(id: string) {
    return Db.remove(TABLE, { id })
  }
}
