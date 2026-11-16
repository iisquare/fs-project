import type {
  AgentInteractionToolCall,
  AgentInteractionToolScope,
  AgentKind
} from '@renderer/db/AgentDb'

export interface AgentToolDisplay {
  scope: AgentInteractionToolScope
  label: string
  category?: string
}

type InteractionCallMeta = Pick<AgentInteractionToolCall, 'name' | 'scope' | 'label' | 'category'>

function fallback(agent: AgentKind | null | undefined): AgentToolDisplay {
  if ('project_tools' === agent) {
    return { scope: 'server', label: '', category: '项目工具' }
  }
  if ('browser' === agent) {
    return { scope: 'local', label: '', category: '浏览器操作' }
  }
  if ('desktop' === agent) {
    return { scope: 'local', label: '', category: '桌面操作' }
  }
  return { scope: 'local', label: '' }
}

/**
 * 子 Agent 详情页展示的是它实际调用的工具，而不是再次展示子 Agent 本身。
 * 新记录直接使用持久化的执行边界与类型；旧记录按所属 Agent 做保守回退。
 */
export function agentToolDisplay(
  call: InteractionCallMeta,
  agent: AgentKind | null | undefined
): AgentToolDisplay {
  const defaults = fallback(agent)
  const category = String(call.category ?? '').trim() || defaults.category
  return {
    scope: call.scope ?? defaults.scope,
    label: String(call.label ?? '').trim() || call.name,
    ...(category ? { category } : {})
  }
}
