/**
 * 项目能力注册表。
 *
 * 项目只保存已启用能力的标识，避免每增加一种能力就新增数据库列、表单字段和运行时判断。
 * 子 Agent 与输出增强共用同一份集合，未来只追加定义即可。
 */
export const PROJECT_CAPABILITY = {
  PROJECT_TOOLS: 'sub_agent.project_tools',
  BROWSER: 'sub_agent.browser',
  DESKTOP: 'sub_agent.desktop',
  ECHARTS: 'output.echarts'
} as const

export type ProjectCapabilityGroup = 'subAgent' | 'enhancement'

export interface ProjectCapabilityDefinition {
  id: string
  group: ProjectCapabilityGroup
  title: string
  description: string
}

export const PROJECT_CAPABILITY_DEFINITIONS: ProjectCapabilityDefinition[] = [
  {
    id: PROJECT_CAPABILITY.PROJECT_TOOLS,
    group: 'subAgent',
    title: '项目工具 Agent',
    description:
      '开启且有已启用的项目工具时，由独立子 Agent 执行；关闭时这些工具由主 Agent 直接调用。'
  },
  {
    id: PROJECT_CAPABILITY.BROWSER,
    group: 'subAgent',
    title: '浏览器 Agent',
    description: '允许主 Agent 委派网页浏览、点击、填表、登录辅助、信息提取与截图任务。'
  },
  {
    id: PROJECT_CAPABILITY.DESKTOP,
    group: 'subAgent',
    title: '桌面 Agent',
    description:
      '允许主 Agent 委派本机桌面截图、窗口与鼠标键盘操作；仍需在系统设置中启用桌面子 Agent，并按桌面访问策略授权。'
  },
  {
    id: PROJECT_CAPABILITY.ECHARTS,
    group: 'enhancement',
    title: 'ECharts 图表',
    description:
      '启用后在系统提示词中追加图表输出规范，让模型知道可以用 echarts 代码块输出数据图表。'
  }
]

/** 旧项目配置的子 Agent 开关：只在读取 / 导入时转换，新配置不再保存这些字段 */
export interface LegacyProjectCapabilitySource {
  browserAgentEnabled?: unknown
  projectToolAgentEnabled?: unknown
  desktopAgentEnabled?: unknown
}

export interface ProjectCapabilitySource extends LegacyProjectCapabilitySource {
  capabilities?: unknown
}

function enabled(value: unknown): boolean {
  return true === value || 1 === value || '1' === value || 'true' === value
}

/** 归一能力集合：保留未知标识以兼容新版本配置，并把旧子 Agent 开关合并进来 */
export function normalizeCapabilities(
  source: ProjectCapabilitySource | null | undefined
): string[] {
  const result = new Set<string>()
  if (Array.isArray(source?.capabilities)) {
    for (const value of source.capabilities) {
      if ('string' === typeof value && value.trim()) result.add(value.trim())
    }
  }
  if (enabled(source?.projectToolAgentEnabled)) result.add(PROJECT_CAPABILITY.PROJECT_TOOLS)
  if (enabled(source?.browserAgentEnabled)) result.add(PROJECT_CAPABILITY.BROWSER)
  if (enabled(source?.desktopAgentEnabled)) result.add(PROJECT_CAPABILITY.DESKTOP)
  return Array.from(result)
}

export function hasCapability(
  source: ProjectCapabilitySource | null | undefined,
  id: string
): boolean {
  return normalizeCapabilities(source).includes(id)
}

export const ECHARTS_CAPABILITY_PROMPT = `## 可视化输出
数据适合用图表表达时，可在正文中插入一个或多个 \`\`\`echarts 代码块。每块只包含一个合法 JSON ECharts option，数据必须来自上下文。标题使用 title.text，副标题使用 title.subtext（不要写成 title.subtitle）。无合适数据或文字已足够时不输出图表，禁止函数、注释和外部脚本。

示例：
\`\`\`echarts
{"title":{"text":"各部门人数","subtext":"共 39 人"},"xAxis":{"type":"category","data":["研发","市场","运营"]},"yAxis":{"type":"value"},"series":[{"type":"bar","data":[18,12,9]}]}
\`\`\``

/** 按启用的能力组装自动追加的系统提示词片段 */
export function capabilityPrompt(source: ProjectCapabilitySource | null | undefined): string {
  return hasCapability(source, PROJECT_CAPABILITY.ECHARTS) ? ECHARTS_CAPABILITY_PROMPT : ''
}
