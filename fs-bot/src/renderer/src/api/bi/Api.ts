import api from '@renderer/core/Api'

/**
 * BI 模块（fs-java web:bi）的接口入口。
 *
 * 桌面端只读：项目里可以把数据主题作为一种工具引用，主题的配置与查询都在服务端。
 */
export default {
  get(url: string, params: Record<string, any> = {}, tips = {}) {
    return api.get('/bi' + url, params, tips)
  },
  post(url: string, data: any = {}, tips = {}) {
    return api.post('/bi' + url, data, tips)
  }
}
