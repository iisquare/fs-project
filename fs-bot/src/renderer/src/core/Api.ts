/* eslint-disable @typescript-eslint/no-explicit-any -- Axios 配置与业务响应在网关层保持透传 */
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
      ax.request(options)
        .then((response: any) => {
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
          if (ApiUtil.code(result) === 90426) {
            window.dispatchEvent(new CustomEvent('fs-client-upgrade-required'))
          }
          if (ApiUtil.succeed(result)) {
            resolve(result)
          } else {
            reject(result)
          }
        })
        .catch((error: any) => {
          const result = error?.response?.data
          if (ApiUtil.code(result) === 90426) {
            window.dispatchEvent(new CustomEvent('fs-client-upgrade-required'))
          }
          tips.error && ElMessage.error(ApiUtil.message(result) || error.message)
          reject(result || ApiUtil.result(500, error.message, error))
        })
    })
  },

  async request(tips: any, options: any) {
    const user = useUserStore()
    const client = await window.ipc.clientMeta()
    if (!options.headers) {
      options.headers = {}
    }
    Object.assign(options.headers, {
      Accept: 'application/json',
      'x-client-version': client.clientVersion,
      'x-client-platform': client.platform,
      'x-client-arch': client.architecture,
      'x-client-id': client.clientId,
      'x-client-channel': client.channel
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
