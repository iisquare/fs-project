import api from '@renderer/core/Api'

/**
 * 智能体模块（fs-java web:agent）的接口入口。
 *
 * 桌面端的后端能力全部走 agent 模块：编排应用、会话、技能、工具都在这里，
 * 应用不直连数据库、也不自己持有工具凭据（工具调用由服务端完成）。
 */
export default {
  get(url: string, params: Record<string, unknown> = {}, tips = {}) {
    return api.get('/agent' + url, params, tips)
  },
  post(url: string, data: unknown = {}, tips = {}, options: Record<string, unknown> = {}) {
    return api.post('/agent' + url, data, tips, options)
  }
}
