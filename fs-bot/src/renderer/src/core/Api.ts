import axios from 'axios'
import ApiUtil from '@renderer/utils/ApiUtil'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@renderer/stores/user'

const $axios = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL,
  withCredentials: true
})

export default {
  $axios,
  fetch(ax: any, tips: any, options: any) {
    tips = Object.assign({}, { success: false, warning: true, error: true }, tips ?? {})
    return new Promise((resolve, reject) => {
      ax.request(options).then((response: any) => {
        let result = response.data
        if (!result) {
          tips.error && ElMessage.error('获取请求结果异常')
          result = ApiUtil.result(500, '获取请求结果异常', result)
        }
        if (ApiUtil.succeed(result)) {
          tips.success &&
            ElMessage({
              message: ApiUtil.message(result),
              type: 'success'
            })
        } else if (tips.warning) {
          ElMessage({
            message: ApiUtil.message(result),
            type: 'warning'
          })
        }
        if (ApiUtil.succeed(result)) {
          resolve(result)
        } else {
          reject(result)
        }
      })
      .catch((error: any) => {
        tips.error && ElMessage.error(error.message)
        reject(ApiUtil.result(500, error.message, error))
      })
    })
  },

  async request(tips: any, options: any) {
    const user = useUserStore()
    if (!options.headers) {
      options.headers = {}
    }
    Object.assign(options.headers, {
      Accept: 'application/json'
    })
    if (user.info.token) {
      Object.assign(options.headers, {
        Authorization: 'Bearer ' + user.info.token
      })
    }
    return await this.fetch($axios, tips, options)
  },

  get(url: string, params = {}, tips = {}, options = {}) {
    return this.request(tips, Object.assign(options, { method: 'GET', url, params }))
  },

  post(url: string, data: any = null, tips = {}, options = {}) {
    return this.request(tips, Object.assign(options, { method: 'POST', url, data }))
  }
}
