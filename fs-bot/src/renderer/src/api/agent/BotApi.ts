import base from './Api'

/**
 * 本地客户端接口（服务端 agent 模块的 bot 模块）：客户端自用的模型清单与模型对话。
 *
 * 与编排（agentic）、模型对比（compare）互不依赖：客户端需要的口径都在这里，
 * 不必借道调试功能的接口与权限。
 */
export default {
  /** 可用模型清单：返回 `{ rows: [{ name, ownedBy, contextLength }] }`（contextLength 为上下文长度，0 表示未配置） */
  models(tips = {}) {
    return base.post('/bot/models', {}, tips)
  },
  /**
   * 当前登录用户的积分概览：总消耗、剩余，以及各积分限流周期的已用量、剩余与下次重置时间。
   * 未开通积分时返回 `available: false`，不会把账号状态当成接口异常。
   */
  credit(tips = {}) {
    return base.post('/bot/credit', {}, tips)
  },
  /**
   * 文件上传地址：本地文件由客户端读出来按 multipart 直传服务端，拿回文件标识等信息。
   * 拿到 id 之后就能交给服务端的工具方法（如 multimodalAnalyze）按 id 引用该文件。
   */
  uploadUrl(): string {
    const endpoint = String(import.meta.env.VITE_APP_API_URL || '').replace(/\/+$/, '')
    return endpoint + '/agent/bot/upload'
  },
  /** 模型对话地址：会话历史由客户端带上，服务端逐段推送增量（SSE，报文为 `{ type, data }`） */
  chatUrl(): string {
    const endpoint = String(import.meta.env.VITE_APP_API_URL || '').replace(/\/+$/, '')
    return endpoint + '/agent/bot/chat'
  },
  /**
   * 服务端工具定义：把项目里选中的工具（工具方法 / 知识库 / 数据主题 / 知识图谱本体 / 编排应用）
   * 补全成模型可见的 function 定义；本地工具（文件与命令）由客户端自己定义，不走这里。
   * 返回 `{ rows: [{ ref, function }] }`
   */
  tools(tools: any[], tips = {}) {
    return base.post('/bot/tools', { tools }, tips)
  },
  /**
   * 执行一次服务端工具调用：`{ tool: ref, args }`，返回 `{ data: <工具返回> }`。
   * 客户端驱动调用循环，凭据与真实调用都留在服务端。
   */
  tool(tool: any, args: any, tips = {}) {
    return base.post('/bot/tool', { tool, args }, tips)
  }
}
