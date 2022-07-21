import axios from 'axios'
import notification from 'ant-design-vue/es/notification'
import modal from 'ant-design-vue/es/modal'
import ApiUtil from '@/utils/api'

// @link:https://www.npmjs.com/package/axios
const $axios = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL,
  withCredentials: true
})

// axios.interceptors.response.use((response) => {
//   return Promise.resolve(response)
// }, (error) => {
//   return Promise.reject(error)
// })

export default {
  $axios,
  wrapper (ax, tips, config) {
    tips = Object.assign({}, { success: false, warning: true, error: true }, tips ?? {})
    return new Promise((resolve, reject) => {
      ax.request(config).then((response) => {
        let result = response.data
        if (!result) { // 请求成功，但服务端未返回内容，可能是序列化失败所致
          result = ApiUtil.result(500, '获取请求结果异常', result)
        }
        if (result[ApiUtil.FIELD_CODE] === 403 && result[ApiUtil.FIELD_MSG] === 'required login') {
          modal.confirm({
            title: '操作提示',
            content: '登录状态已失效，是否前往登录页面重新登录？',
            onOk () {
              window.location.reload()
            }
          })
        } else if (result[ApiUtil.FIELD_CODE] === 0) {
          tips.success && notification.success({
            message: '状态：' + result[ApiUtil.FIELD_CODE],
            description: '消息:' + result[ApiUtil.FIELD_MSG]
          })
        } else if (tips.warning) {
          notification.warning({
            message: '状态：' + result[ApiUtil.FIELD_CODE],
            description: '消息:' + result[ApiUtil.FIELD_MSG]
          })
        }
        resolve(result)
      }).catch((error) => {
        if (tips.error) {
          notification.error({ message: '请求异常', description: error.message })
        }
        resolve(ApiUtil.result(500, error.message, error))
      })
    })
  },
  request (tips, config) {
    return this.wrapper($axios, tips, config)
  },
  get (url, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'get', url }))
  },
  post (url, data = null, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'post', url, data }))
  }
}
