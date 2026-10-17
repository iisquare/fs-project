import type { Project } from '@renderer/db/ProjectDb'

/**
 * 项目配置文件：导入 / 导出与「从分享创建本地项目」共用同一份格式。
 *
 * ```
 * {
 *   "type": "fs-bot-project",      // 格式标识，导入时据此识别
 *   "version": 1,                  // 格式版本，将来结构变化时递增
 *   "exportedAt": "2026-10-14T...",// 导出时间（仅记录用）
 *   "share": { ... } | null,       // 从分享导出的文件才带来源信息
 *   "project": { ... }             // 项目配置本身（不含本地 id 与时间戳）
 * }
 * ```
 *
 * 导入对格式宽容：`project` 缺失时把整个文件当配置读，字段缺失按默认值补齐，
 * 这样手工写的、旧版本导出的、管理端导出的文件都能进来。
 */

export const PROJECT_FILE_TYPE = 'fs-bot-project'
export const PROJECT_FILE_VERSION = 1

/** 配置文件里的项目配置：本地标识（id）与时间戳不导出，导入时重新生成 */
export interface ProjectConfig {
  model: string
  contextLength: number
  systemPrompt: string
  /**
   * 长期记忆：与系统提示词分开存放的一段文字，导出 / 分享 / 导入都随项目配置一起走。
   * 只有 `memoryEnabled` 为 true 时才会随对话下发。
   */
  memory: string
  /** 是否启用客户端记忆：默认关闭；旧配置文件里没有这一项时按关闭处理 */
  memoryEnabled: boolean
  thinkEffort: string
  name: string
  description: string
  icon: string
  envVars: any[]
  tools: any[]
  workspace: any[]
  inputs: Record<string, any>
  sort: number
}

/** 配置文件里的来源信息：从分享导出的文件才有 */
export interface ProjectFileShare {
  projectId: number
  projectName: string
  versionId: number
  versionName: string
  description: string
}

export interface ProjectFile {
  type: string
  version: number
  exportedAt: string
  share: ProjectFileShare | null
  project: ProjectConfig
}

function text(value: unknown): string {
  return typeof value === 'string'
    ? value
    : value === null || value === undefined
      ? ''
      : String(value)
}

function list(value: unknown): any[] {
  return Array.isArray(value) ? value : []
}

function record(value: unknown): Record<string, any> {
  return value && 'object' === typeof value && !Array.isArray(value)
    ? (value as Record<string, any>)
    : {}
}

/** 配置里的布尔开关：只认明确的真值，缺失 / 旧格式一律按关闭处理 */
function flag(value: unknown): boolean {
  return true === value || 1 === value || '1' === value || 'true' === value
}

/** 归一成配置对象：字段缺失按默认值补齐，类型不对的一律退回默认值 */
export function configOf(source: Partial<Project> | Record<string, any>): ProjectConfig {
  return {
    model: text(source?.model).trim(),
    contextLength: Math.max(0, Math.floor(Number(source?.contextLength ?? 0)) || 0),
    systemPrompt: text(source?.systemPrompt),
    memory: text(source?.memory),
    memoryEnabled: flag(source?.memoryEnabled),
    thinkEffort: text(source?.thinkEffort),
    name: text(source?.name),
    description: text(source?.description),
    icon: text(source?.icon),
    envVars: list(source?.envVars),
    tools: list(source?.tools),
    workspace: list(source?.workspace),
    inputs: record(source?.inputs),
    sort: Number(source?.sort ?? 0) || 0
  }
}

/** 组装导出内容：share 为空表示本机导出的配置（不是从分享来的） */
export function projectFile(config: ProjectConfig, share: ProjectFileShare | null): ProjectFile {
  return {
    type: PROJECT_FILE_TYPE,
    version: PROJECT_FILE_VERSION,
    exportedAt: new Date().toISOString(),
    share,
    project: config
  }
}

/**
 * 发布到服务端的配置：环境变量里**标了「密钥」的行整行留下本机**。
 *
 * 分享内容在服务端是明文存储的（凡拿到这份分享的人都能读到），而环境变量正是放 token、
 * 密码这类东西的地方——勾了「密钥」就是把「这个值只留本机」说清楚了，发布时照它办。
 * 其余环境变量（JDK 路径、工具链目录这类）随配置一起走，取用的人按自己的机器改。
 */
export function publishConfig(config: ProjectConfig): ProjectConfig {
  return { ...config, envVars: list(config.envVars).filter((item) => true !== item?.secret) }
}

/** 默认文件名：`<项目名>-v<版本>.json`，去掉文件系统不允许的字符 */
export function filenameOf(name: string, version = ''): string {
  const base = text(name)
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
  const suffix = text(version)
    .replace(/[\\/:*?"<>|]/g, '-')
    .trim()
  return `${base || '项目配置'}${suffix ? `-v${suffix}` : ''}.json`
}

/** 导入解析：文件内容 → 配置对象与来源信息；内容不是配置时抛错，由调用方提示 */
export function parseProjectFile(content: string): {
  project: ProjectConfig
  share: ProjectFileShare | null
} {
  let parsed: any
  try {
    parsed = JSON.parse(content)
  } catch {
    throw new Error('文件不是合法的 JSON，无法作为项目配置导入')
  }
  if (!parsed || 'object' !== typeof parsed || Array.isArray(parsed)) {
    throw new Error('文件内容不是项目配置对象')
  }
  // 标准格式把配置放在 project 下；直接给配置对象（手工写的文件）也照收
  const raw = parsed.project && 'object' === typeof parsed.project ? parsed.project : parsed
  const project = configOf(raw)
  if (!project.name && !project.model) {
    throw new Error('文件里既没有项目名称也没有模型，无法识别为项目配置')
  }
  const share = parsed.share && 'object' === typeof parsed.share ? parsed.share : null
  return {
    project,
    share: share
      ? {
          projectId: Math.max(0, Math.floor(Number(share.projectId ?? 0)) || 0),
          projectName: text(share.projectName),
          versionId: Math.max(0, Math.floor(Number(share.versionId ?? 0)) || 0),
          versionName: text(share.versionName),
          description: text(share.description)
        }
      : null
  }
}

/** 导入时的重名处理：同名项目加「（导入）」后缀，再冲突就按序号往下排 */
export function uniqueName(name: string, names: string[]): string {
  const base = text(name).trim() || '未命名项目'
  if (!names.includes(base)) return base
  let candidate = `${base}（导入）`
  let index = 2
  while (names.includes(candidate)) {
    candidate = `${base}（导入 ${index}）`
    index++
  }
  return candidate
}
