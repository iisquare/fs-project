import axios from 'axios'
import ApiUtil from '@/utils/ApiUtil'
import { ElNotification, ElMessageBox } from 'element-plus'

const $axios = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL,
  withCredentials: true
})

export default {
  $axios,
  wrapper(ax: any, tips: any, config: any) {
    tips = Object.assign({}, { success: false, warning: true, error: true }, tips ?? {})
    return new Promise((resolve, reject) => {
      ax.request(config).then((response: any) => {
        let result = response.data
        if (!result) {
          tips.error && ElNotification({
            title: '获取请求结果异常',
            message: '请求成功，但服务端未返回内容，可能是序列化失败所致',
            type: 'error',
          })
          result = ApiUtil.result(500, '获取请求结果异常', result)
        }
        if (ApiUtil.code(result) === 403 && ApiUtil.message(result) === 'required login') {
          ElMessageBox.confirm('登录状态已失效，是否前往登录页面重新登录？', '操作提示', { type: 'warning', }).then(() => {
            window.location.reload()
          }).catch(() => {})
        } else if (ApiUtil.code(result) === 0) {
          tips.success && ElNotification({
            title: '状态：' + ApiUtil.code(result),
            message: '消息：' + ApiUtil.message(result),
            type: 'success',
          })
        } else if (tips.warning) {
          ElNotification({
            title: '状态：' + ApiUtil.code(result),
            message: '消息：' + ApiUtil.message(result),
            type: 'warning',
          })
        }
        resolve(result)
      }).catch((error: any) => {
        tips.error && ElNotification({
          title: '请求异常',
          message: error.message,
          type: 'error',
        })
        resolve(ApiUtil.result(500, error.message, error))
      })
    })
  },
  request(tips: any, config: any) {
    return this.wrapper($axios, tips, config)
  },
  get(url: string, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'get', url }))
  },
  post(url: string, data: any = null, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'post', url, data }))
  },
}
