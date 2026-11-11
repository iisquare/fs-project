/**
 * 智能体流式接口的「端点 + 协议适配」注册表：
 * 页面只说用哪条流，uri 与报文解析都留在这里，协议知识不再散落到视图里。
 *
 * 新增一条流时：在这里补一个端点（uri + parse），页面 `...streams.xxx` 即可复用
 * useAgenticStream 的运行状态、兜底与中断语义。
 *
 * @example
 * const stream = useAgenticStream({ ...streams.agenticInvoke, headers, fallback: 'invoke', ... })
 */
import type { AgenticStreamEndpoint, AgenticStreamEvent } from '@/types/agent'

/** 编排接口的默认协议：`{ type: delta|step|round|done|error, data }` */
const typed = (payload: any): AgenticStreamEvent[] => {
  const types: AgenticStreamEvent['type'][] = ['delta', 'step', 'round', 'done', 'error']
  return types.indexOf(payload?.type) >= 0 ? [payload as AgenticStreamEvent] : []
}

export default {
  /** 发布版流程对话：同一公开接口通过 stream=true 返回增量、节点进度与 ReAct 轮次 */
  agenticInvoke: {
    uri: '/agentic/invoke',
    streamParams: { stream: true },
    parse: typed,
  } as AgenticStreamEndpoint,

  /** 编排页运行抽屉：同一套协议，只有 uri 不同（/agentic/runStream） */
  agenticRun: { uri: '/agentic/runStream', parse: typed } as AgenticStreamEndpoint,

  /**
   * 模型对比：一条连接只跑一个模型（参数逐模型独立，所以不能合并成一条流）。
   * 报文是 `{ action, data }`（与编排那套不同），把 finish_reason 一并交给页面——
   * 对比时「为什么停下来」和输出内容一样是要看的。
   */
  chatCompare: {
    uri: '/compare/stream',
    parse: (payload: any): AgenticStreamEvent[] => {
      if ('choices.message' === payload?.action) {
        return (payload.data ?? []).map((item: any) => {
          const delta: any = item?.delta || item?.message || {}
          return {
            type: 'delta' as const,
            data: {
              reasoning: delta.reasoning_content,
              content: delta.content,
              finishReason: item?.finish_reason,
              usage: item?.usage,
            },
          }
        })
      }
      if (String(payload?.action ?? '').indexOf('error') === 0) {
        return [{
          type: 'error' as const,
          data: { message: payload.data?.error?.message || payload.data?.message || '模型调用失败' },
        }]
      }
      return []
    },
  } as AgenticStreamEndpoint,
}
