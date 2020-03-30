import axios from 'axios'
import notification from 'ant-design-vue/es/notification'
import modal from 'ant-design-vue/es/modal'

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
    if (tips !== null) {
      tips = Object.assign({}, { success: false, warning: true, error: true }, tips)
    }
    return new Promise((resolve, reject) => {
      ax.request(config).then((response) => {
        if (tips === null) {
          resolve(response)
        } else {
          const result = response.data
          if (result && result.code === 403 && result.message === 'required login') {
            modal.confirm({
              title: '操作提示',
              content: '登录状态已失效，是否前往登录页面重新登录？',
              onOk () {
                window.location.reload()
              }
            })
          } else if (result && result.code === 0) {
            tips.success && notification.success({
              message: '状态：' + result.code,
              description: '消息:' + result.message
            })
          } else if (tips.warning) {
            notification.warning({
              message: '状态：' + result.code,
              description: '消息:' + result.message
            })
          }
          resolve(result)
        }
      }).catch((error) => {
        if (tips === null) {
          reject(error)
        } else {
          if (tips.error) {
            notification.error({
              message: '请求异常',
              description: error.message
            })
          }
          resolve({ code: 500, message: error.message, data: error })
        }
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
