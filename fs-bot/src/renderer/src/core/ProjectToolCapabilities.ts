import type { ProjectTool } from '@renderer/db/ProjectDb'

/** 项目工具类型的固定展示顺序；不随项目配置中的添加顺序变化。 */
const PROJECT_TOOL_KIND_ORDER = ['method', 'knowledge', 'theme', 'ontology', 'agentic'] as const

export const PROJECT_TOOL_KIND_LABELS: Record<string, string> = {
  method: '工具方法',
  knowledge: '知识库',
  theme: '数据主题',
  ontology: '知识图谱',
  agentic: '编排应用'
}

const MAX_SUMMARY_ITEMS = 8
const MAX_SUMMARY_NAME_CHARS = 48
const MAX_SUMMARY_DESCRIPTION_CHARS = 72

function oneLine(value: unknown, limit: number): string {
  const text = String(value ?? '')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length <= limit ? text : `${text.slice(0, Math.max(0, limit - 1)).trimEnd()}…`
}

function toolTitle(tool: ProjectTool): string {
  return oneLine(tool.title || tool.name || tool.methodName, MAX_SUMMARY_NAME_CHARS)
}

/**
 * 主 Agent 的工具路由摘要。
 *
 * 这里只暴露实际启用到的类型、数量和少量代表能力，帮助主 Agent 判断是否需要委派；
 * 完整的方法说明与参数 schema 仍由 Project Tool Agent 在独立上下文中读取。
 */
export function projectToolCapabilitySummary(tools: readonly ProjectTool[]): string {
  const enabled = tools.filter(
    (tool) => false !== tool?.enabled && Object.hasOwn(PROJECT_TOOL_KIND_LABELS, tool?.kind)
  )
  if (!enabled.length) return ''

  const counts = new Map<string, number>()
  for (const tool of enabled) counts.set(tool.kind, (counts.get(tool.kind) ?? 0) + 1)

  const typeSummary = PROJECT_TOOL_KIND_ORDER.filter((kind) => counts.has(kind))
    .map((kind) => `${PROJECT_TOOL_KIND_LABELS[kind]}（${counts.get(kind)} 个）`)
    .join('、')

  const representatives: string[] = []
  for (const kind of PROJECT_TOOL_KIND_ORDER) {
    for (const tool of enabled) {
      if (tool.kind !== kind || representatives.length >= MAX_SUMMARY_ITEMS) continue
      const name = toolTitle(tool)
      if (!name) continue
      const description = oneLine(tool.description, MAX_SUMMARY_DESCRIPTION_CHARS)
      representatives.push(description && description !== name ? `${name}（${description}）` : name)
    }
  }

  const hidden = enabled.length - representatives.length
  return [
    '当前启用的自定义能力：',
    `- 类型：${typeSummary}`,
    `- 代表能力：${representatives.join('、')}${hidden > 0 ? `；另有 ${hidden} 个未展开` : ''}`
  ].join('\n')
}
