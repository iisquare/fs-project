import { useUserStore } from '@renderer/stores/user'
import BotApi from '@renderer/api/agent/BotApi'
import { SseStream } from './Stream'

/**
 * 模型对话：本地客户端对选定模型的一次直接调用（`POST /agent/bot/chat`）。
 *
 * 服务端只跑这一轮模型调用：会话历史由客户端带上（多轮对话把历史放进 messages），
 * 报文是 `{ type: delta | error, data }`，与编排的流式接口同一套格式，解析沿用 `SseStream`。
 *
 * 这里只负责「一次模型调用」：带不带工具、工具怎么执行、调用链怎么续，都在上层（chat store）决定。
 * 工具调用的分片按 OpenAI 规范原样交给调用方累积（index / id / function.name / function.arguments）。
 *
 * 只有显式 stop 或请求失败才会断开。
 */

export interface ModelChatMessage {
  role: string
  /** 文本内容；带工具调用的助手消息可为空，工具返回消息即返回的文本 */
  content?: string
  /** 助手发起的工具调用：模型给的回调标识、函数名与参数（JSON 文本） */
  toolCalls?: ModelToolCall[]
  /** 工具返回消息：对应哪次调用、哪个函数 */
  toolCallId?: string
  name?: string
}

/** 一次工具调用（发送给模型时用完整形态，流式接收时用分片形态） */
export interface ModelToolCall {
  id: string
  type: string
  function: { name: string; arguments: string }
}

export interface ModelChatDelta {
  content: string
  reasoning: string
  finishReason: string
  /** 本轮模型给出的工具调用分片：同一 index 的多次推送按序拼接 */
  toolCalls?: any[]
}

export interface ModelChatParams {
  model: string
  /** 本轮会话历史（含本轮输入）：服务端不存会话，历史由客户端维护 */
  messages: ModelChatMessage[]
  /** 系统提示词：非空时由服务端作为首条 system 消息补在最前 */
  systemPrompt?: string
  /** 思考强度：low | medium | high | max，留空则不下发，由模型与网关取默认值 */
  thinkEffort?: string
  /** 模型可见的 function 定义清单：本地工具与项目工具归一后一起下发 */
  tools?: any[]
  temperature?: number
  maxTokens?: number
  onDelta: (delta: ModelChatDelta) => void
  onError?: (message: string) => void
  onClose: (result: { aborted: boolean; error?: string }) => void
}

export class ModelChat {
  private stream: SseStream | null = null

  get running(): boolean {
    return this.stream?.running === true
  }

  start(params: ModelChatParams): void {
    if (this.running) return
    const user = useUserStore()
    const stream = new SseStream()
    this.stream = stream
    stream.start({
      url: BotApi.chatUrl(),
      token: user.info.token,
      body: Object.assign(
        {
          model: params.model,
          systemPrompt: params.systemPrompt ?? '',
          messages: params.messages,
          ...(params.tools?.length ? { tools: params.tools } : {})
        },
        // 未填写的推理参数不下发：交由服务端与模型网关取默认值
        params.temperature === undefined ? {} : { temperature: params.temperature },
        params.maxTokens === undefined ? {} : { maxTokens: params.maxTokens },
        params.thinkEffort ? { thinkEffort: params.thinkEffort } : {}
      ),
      onEvent: (event) => {
        if ('delta' === event.type) {
          params.onDelta({
            content: String(event.data?.content ?? ''),
            reasoning: String(event.data?.reasoning ?? ''),
            finishReason: String(event.data?.finishReason ?? ''),
            toolCalls: Array.isArray(event.data?.toolCalls) ? event.data.toolCalls : []
          })
          return
        }
        if ('error' === event.type) params.onError?.(String(event.data?.message ?? '模型调用失败'))
      },
      onClose: (result) => {
        this.stream = null
        params.onClose(result)
      }
    })
  }

  /** 停止生成：已收到的内容保留，不再接收后续增量 */
  stop(): void {
    this.stream?.abort()
  }
}

export default ModelChat
