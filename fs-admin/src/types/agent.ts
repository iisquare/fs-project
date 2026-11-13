/**
 * 智能体对话相关的公共类型：
 * 流式事件（后端约定 + 协议适配归一后的形态）、流式编排选项、一条消息的形态。
 */

/** 流式事件：`{ type: delta|step|round|done|error, data }`，也是 parse 归一后的目标形态 */
export interface AgenticStreamEvent {
  type: 'delta' | 'step' | 'round' | 'done' | 'error'
  data?: any
}

/** 流式接口的端点定义：uri（相对 app）+ 报文适配（把各家协议归一成事件数组） */
export interface AgenticStreamEndpoint {
  uri: string
  /** 仅在建立 SSE 连接时合并到请求体；非流式兜底不携带这些参数 */
  streamParams?: Record<string, any>
  parse: (payload: any) => AgenticStreamEvent[]
}

/** useAgenticStream 的入参 */
export interface AgenticStreamOptions {
  app?: string
  uri?: string
  streamParams?: Record<string, any>
  headers?: Record<string, any>
  fallback?: 'none' | 'invoke'
  invoke?: ((payload: any) => Promise<any>) | null
  parse?: ((payload: any) => AgenticStreamEvent | AgenticStreamEvent[] | null) | null
  onDelta?: (chunk: any) => void
  onStep?: (step: any) => void
  onRound?: (round: any) => void
  onDone?: (data: any) => void
  onError?: (error: { message: string; code?: any; streamed?: boolean }) => void
  onClose?: () => void
}

/** 一条消息（用户提问 / 助手回复 / 独立异常行）：页面与插槽都按它取字段 */
export interface ChatMessageItem {
  id?: any
  /** 父消息标识：0 表示分支起点，其余指向上一轮消息（消息树） */
  parentId?: any
  role?: string
  content?: string
  reasoning?: string
  streaming?: boolean
  notice?: { summary?: string; detail?: string } | null
  createdTime?: number
  logId?: any
  steps?: any[]
  progress?: any[]
  rounds?: any[]
  files?: any[]
  feedbackEmotion?: string
  feedbackTag?: string
  feedbackContent?: string
  [key: string]: any
}
