import api from '@renderer/core/Api'

/**
 * 知识图谱模块（fs-java web:kg）的接口入口。
 *
 * 桌面端只读：项目里可以把本体作为一种工具引用，实体与关联的检索都在服务端。
 */
export default {
  get(url: string, params: Record<string, any> = {}, tips = {}) {
    return api.get('/kg' + url, params, tips)
  },
  post(url: string, data: any = {}, tips = {}) {
    return api.post('/kg' + url, data, tips)
  }
}
