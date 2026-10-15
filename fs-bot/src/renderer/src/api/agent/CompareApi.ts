import base from './Api'

/**
 * 模型对比的模型清单：只作为客户端取模型清单的兜底通道
 * （主通道是 bot 接口 `/bot/models`；服务端还没带上 bot 模块时退回这里）。
 * 模型对话不走这条线，见 `core/ModelChat` 的 `/bot/chat`。
 */
export default {
  /** 可用模型清单：返回 `{ rows: [{ name, ownedBy, contextLength }] }`，取自模型网关 */
  models(tips = {}) {
    return base.post('/compare/models', {}, tips)
  }
}
