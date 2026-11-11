/**
 * 编排流式调用的通用编排（组合式函数）：
 * 把 /agentic/invoke?stream=true、/agentic/runStream 的「事件分发 + 运行状态 + 兜底策略」收敛到一处，
 * 页面只需关心「收到增量怎么画」「结束后做什么」，不再各写一份 FetchEventSource 分支。
 *
 * 事件约定（与后端一致）：`{ type: delta|step|round|done|error, data }`
 * - 报文协议不同的接口（如 /compare/stream 的 `{ action, data }`）由 `parse` 归一成上面的事件数组，
 *   端点与适配统一登记在 `@/api/agent/streams`，页面 `...streams.xxx` 直接复用；
 * - 业务失败（授权被撤销、缺少必填参数等）在流式通道里就是 error 事件，这里统一转给 onError；
 * - 主动中断（页面上的「停止生成」）不算失败：只收尾（onClose），不会触发 onError；
 * - 兜底策略 fallback：
 *   `'none'`   只报错（调试面板：失败就是失败，不重跑）；
 *   `'invoke'` 仅在「一个事件都没收到」时改用一次性接口重跑（接口未更新 / 代理不支持 SSE 的兼容场景），
 *              收到过事件说明服务端已经在跑，重跑会重复消耗工具与模型额度，因此不再兜底。
 *
 * @param {Object} options 见 AgenticStreamOptions（types/agent.ts）
 * @returns {{ running: Ref<boolean>, send: (payload?: any) => void, abort: () => void }}
 * @example
 * const stream = useAgenticStream({
 *   ...streams.agenticInvoke,
 *   headers: { 'X-Auth-Token': user.info.token },
 *   fallback: 'invoke',
 *   invoke: (payload) => AgenticApi.invoke(payload, { success: false, warning: false }),
 *   onDelta: (chunk) => append(chunk),
 *   onStep: (step) => markStep(progress.value, step),
 *   onRound: (round) => markRound(rounds.value, round),
 *   onDone: (data) => applyResult(data),
 *   onError: (error) => attachNotice(error.message),
 * })
 * stream.send({ id, chatId, inputs })
 */
import { onBeforeUnmount, ref } from 'vue'
import ApiUtil from '@/utils/ApiUtil'
import FetchEventSource from '@/core/FetchEventSource'
import type { AgenticStreamEvent, AgenticStreamOptions } from '@/types/agent'

export default function useAgenticStream(options: AgenticStreamOptions = {}) {
  const {
    app = 'agent',
    uri = '',
    streamParams = {},
    headers = {},
    fallback = 'none',
    invoke = null,
    parse = null,
    onDelta = () => {},
    onStep = () => {},
    onRound = () => {},
    onDone = () => {},
    onError = () => {},
    onClose = () => {},
  } = options
  const running = ref(false)
  let sse: FetchEventSource | null = null
  /** 本轮是否收到过事件：兜底只在「一个都没收到」时触发（服务端已在跑时重跑会重复消耗） */
  let streamed = false
  /** 本轮是否由页面主动中断：中断不算失败，只收尾 */
  let aborted = false
  /** 本轮是否已收尾：底层关闭与兜底完成都会走到这里，保证 onClose 只回调一次 */
  let finished = false

  /** 收尾（幂等）：复位运行状态并回调 onClose */
  const finish = () => {
    if (finished) return
    finished = true
    running.value = false
    onClose()
  }

  /** 默认报文解析：与后端约定一致 `{ type: delta|step|round|done|error, data }` */
  const parseDefault = (payload: any): AgenticStreamEvent[] => {
    const types: AgenticStreamEvent['type'][] = ['delta', 'step', 'round', 'done', 'error']
    return types.indexOf(payload?.type) >= 0 ? [payload as AgenticStreamEvent] : []
  }

  /** 事件分发：增量、节点进度、ReAct 轮次、结果、异常各归各的回调 */
  const dispatch = (event: AgenticStreamEvent) => {
    if ('delta' === event?.type) {
      onDelta(event.data ?? {})
      return
    }
    if ('step' === event?.type) {
      onStep(event.data ?? {})
      return
    }
    if ('round' === event?.type) {
      onRound(event.data ?? {})
      return
    }
    if ('error' === event?.type) {
      onError({ message: event.data?.message || '运行失败', code: event.data?.code ?? 0 })
      return
    }
    if ('done' === event?.type) onDone(ApiUtil.data(event.data) ?? event.data ?? {})
  }

  /** 报文入口：解析 JSON → 归一成事件（可以是数组）→ 逐个分发 */
  const handleMessage = (event: any) => {
    if (!event?.data || '[DONE]' === event.data) return
    let payload: any = null
    try {
      payload = JSON.parse(event.data)
    } catch (error) {
      // 不是 JSON：多半是版本不匹配或代理插入了内容，留个痕迹便于排查
      console.warn('[agentic-stream] 报文不是 JSON，已忽略', event.data)
      return
    }
    streamed = true
    const parsed: any = ('function' === typeof parse ? parse(payload) : parseDefault(payload)) ?? []
    ;(Array.isArray(parsed) ? parsed : [parsed]).forEach(dispatch)
  }

  /** 开始一轮：先订阅事件再发送，结束（成功或失败）统一复位 running 并回调 onClose */
  const send = (payload: any = {}) => {
    if (running.value) return
    running.value = true
    streamed = false
    aborted = false
    finished = false
    sse = new FetchEventSource(app, uri)
    if (headers) sse.addHeaders(headers)
    sse.onMessage(handleMessage)
    sse.onError((error: any) => {
      // 页面主动中断：不当成失败（内容已上屏），底层 finally 仍会回调 onClose 收尾
      if (aborted) {
        running.value = false
        return
      }
      const message = error?.message || (error?.status ? `状态码 ${error.status}` : '运行失败')
      // 已收到过事件：服务端仍在继续执行，只提示不重跑；完全没收到才走一次性兜底
      if ('invoke' === fallback && !streamed && 'function' === typeof invoke) {
        invoke(payload).catch(() => {}).finally(finish)
        return
      }
      // streamed=true 表示本轮已在服务端开跑（页面据此提示「结果仍会落库」，而不是提示接口不可用）
      onError({ message, code: error?.status ?? 0, streamed })
      finish()
    })
    sse.onClose(finish)
    // streamParams 只进入 SSE 请求；fallback 仍使用原始 payload，确保一次性调用返回 JSON
    sse.send(Object.assign({}, payload, streamParams))
  }

  /** 中断：保留已经上屏的内容，只是不再接收后续事件（不算失败，不触发 onError） */
  const abort = () => {
    aborted = true
    sse?.abort()
    running.value = false
  }
  onBeforeUnmount(abort)

  return { running, send, abort }
}
