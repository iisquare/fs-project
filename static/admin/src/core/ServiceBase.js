import axios from 'axios'
import notification from 'ant-design-vue/es/notification'

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
  request (tips, config) {
    if (tips !== null) {
      tips = Object.assign({}, { showSuccess: false, showWarning: true, showError: true }, tips)
    }
    return new Promise((resolve, reject) => {
      $axios.request(config).then((response) => {
        if (tips === null) {
          resolve(response)
        } else {
          const result = response.data
          if (result && result.code === 403 && result.message === 'required login') {
            window.location.reload()
          } else if (result && result.code === 0) {
            tips.showSuccess && notification.success({
              message: '状态：' + result.code,
              description: '消息:' + result.message
            })
          } else if (tips.showWarning) {
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
          if (tips.showError) {
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
  get (url, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'get', url }))
  },
  post (url, data = null, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'post', url, data }))
  }
}
