/**
 * 模型对比的运行器：一次提交并发跑多个模型，每个模型一条独立的流式连接。
 *
 * 为什么不用 useAgenticStream：那个组合式函数管的是「一条连接」的运行状态，
 * 这里需要 N 条并发且要能单独中断，所以自己维护连接表；协议解析仍然复用
 * @/api/agent/streams 的端点定义，报文知识不落到视图里。
 *
 * 输出不留在这里：每条流把 runId 与本地 key 带下去，服务端按同一份增量自己攒记录，
 * 所以页面被卸载（切页面 / 刷新）时中断连接，只是不再接收增量，历史里那一条照样在。
 *
 * 结果对象一旦创建就不再替换，流式过程中只改字段（reasoning / content / state），
 * 这样页面绑定的引用始终有效。注意必须先赋值再取用：map 出来的原始对象改了不会触发视图更新。
 */
import { onBeforeUnmount, ref } from 'vue'
import FetchEventSource from '@/core/FetchEventSource'
import streams from '@/api/agent/streams'
import { useUserStore } from '@/stores/user'
import type { AgenticStreamEvent } from '@/types/agent'
import type { CompareGlobalConfig, CompareModelConfig, CompareResult } from '@/types/compare'

/**
 * 思考强度是否随请求下发：自身开关关掉、或思考模式被显式关成 off 时不发。
 * 与设计器里「思考关闭时强度不可编辑」的口径一致。
 */
const effortApplied = (item: CompareModelConfig) => {
  if (!item.thinkEffortEnabled) return false
  if (item.thinkModeEnabled && 'off' === item.thinkMode) return false
  return true
}

/** 组装单个模型的请求体：只用启用过的字段，没配的交给服务端取默认值 */
const buildPayload = (item: CompareModelConfig, global: CompareGlobalConfig, runId: any) => {
  const payload: Record<string, any> = {
    runId: runId ?? 0, // 本轮运行记录标识：服务端据此把这条输出写进对应记录，0 表示不留档
    key: item.key, // 页面本地键：服务端按它把明细行与这张卡片对上
    model: item.model,
    systemPrompt: global.systemPrompt,
    input: global.input,
    stream: global.stream,
    think: global.think,
  }
  if (item.temperatureEnabled) payload.temperature = item.temperature
  if (item.maxTokensEnabled) payload.maxTokens = item.maxTokens
  if (item.thinkModeEnabled) payload.thinkMode = item.thinkMode
  if (effortApplied(item)) payload.thinkEffort = item.thinkEffort
  return payload
}

export default function useModelCompare() {
  const user = useUserStore()
  /** 本轮各模型的结果，顺序与提交时的模型顺序一致 */
  const results = ref<CompareResult[]>([])
  const running = ref(false)
  const sources: { key: string, source: FetchEventSource }[] = []

  /** 单条增量：思考与正文各归各的字段 */
  const applyEvent = (result: CompareResult, event: AgenticStreamEvent) => {
    if ('delta' === event?.type) {
      const data: any = event.data ?? {}
      if (data.reasoning) result.reasoning += String(data.reasoning)
      if (null != data.content) {
        if (!result.firstTokenTime && !result.content) result.firstTokenTime = Date.now()
        result.content += String(data.content)
      }
      if (data.finishReason) result.finishReason = String(data.finishReason)
      if ('idle' === result.state || 'connect' === result.state) result.state = 'streaming'
      return
    }
    // 业务失败（模型不可用、参数非法等）：本轮到此结束
    if ('error' === event?.type) {
      result.state = 'error'
      result.error = event.data?.message || '模型调用失败'
      return
    }
    // done 只做结束标记，真正的收尾在 onClose 里（成功失败都会走那条路径）
    if ('done' === event?.type && !result.finishReason) result.finishReason = 'stop'
  }

  const abortAll = () => {
    sources.splice(0).forEach((item) => item.source.abort())
    running.value = false
  }

  /** 清空上一轮结果（不影响工作区配置） */
  const reset = () => {
    abortAll()
    results.value = []
  }

  /**
   * 发起一轮：每个模型一条连接，各自带着自己的参数与本轮记录标识。
   * runId 由调用方先建档拿到（见 CompareApi.runCreate），这里只负责把它带下去。
   * 返回是否真的发出（上一轮还在跑时直接忽略）。
   */
  const start = (targets: CompareModelConfig[], global: CompareGlobalConfig, runId: any) => {
    if (running.value) return false
    results.value = targets.map((item) => ({
      key: item.key,
      model: item.model,
      state: 'idle',
      content: '',
      reasoning: '',
      finishReason: '',
      error: '',
      payload: buildPayload(item, global, runId),
      createdTime: 0,
      firstTokenTime: 0,
      finishedTime: 0,
    }))
    if (!results.value.length) return false
    running.value = true
    sources.splice(0)
    let pending = results.value.length
    const settle = () => {
      pending -= 1
      if (pending <= 0) running.value = false
    }
    // 从 results.value 里取，拿到的是响应式代理；改原始对象不会触发视图更新
    results.value.forEach((result) => {
      const source = new FetchEventSource('agent', streams.chatCompare.uri)
      source.addHeaders({ 'X-Auth-Token': user.info.token })
      source.onOpen(() => {
        result.state = 'connect'
      }).onMessage((message: any) => {
        if (!message?.data || '[DONE]' === message.data) return
        let payload: any = null
        try {
          payload = JSON.parse(message.data)
        } catch (error) {
          // 不是 JSON：多半是代理插了内容，留个痕迹便于排查
          console.warn('[compare] 报文不是 JSON，已忽略', message.data)
          return
        }
        const parsed = streams.chatCompare.parse(payload) ?? []
        ;(Array.isArray(parsed) ? parsed : [parsed]).forEach((event: any) => applyEvent(result, event))
      }).onError((error: any) => {
        // 主动中断不算失败：内容已经上屏，保持 abort 状态
        if ('abort' === result.state) return
        result.state = 'error'
        result.error = error?.message || (error?.status ? `状态码 ${error.status}` : '调用失败')
      }).onClose(() => {
        result.finishedTime = Date.now()
        if ('error' !== result.state && 'abort' !== result.state) result.state = 'finish'
        settle()
      })
      sources.push({ key: result.key, source })
      result.createdTime = Date.now()
      source.send(result.payload)
    })
    return true
  }

  /** 停单个模型：先落状态再断开，避免中断被 onError 记成失败 */
  const stopOne = (key: string) => {
    const index = sources.findIndex((item) => item.key === key)
    if (index < 0) return
    const result = results.value.find((item) => item.key === key)
    if (result) result.state = 'abort'
    sources.splice(index, 1)[0].source.abort()
  }

  /** 停全部：逐个走 stopOne，保持每个结果的状态正确 */
  const stop = () => {
    sources.map((item) => item.key).forEach(stopOne)
    running.value = false
  }

  onBeforeUnmount(abortAll)

  return { results, running, start, stop, stopOne, reset }
}
