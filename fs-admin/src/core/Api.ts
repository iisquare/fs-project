import axios from 'axios'
import ApiUtil from '@/utils/ApiUtil'
import { ElMessage, ElNotification } from 'element-plus'
import { useCounterStore } from '@/stores/counter'
import ElementUtil from '@/utils/ElementUtil'
import ApiResponseNotice from '@/components/Feedback/ApiResponseNotice.vue'
import { h, nextTick } from 'vue'

const $axios = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL,
  withCredentials: true
})

const NOTICE_DURATION = 4500
let noticeSerial = 0

/**
 * 业务提示默认保持原有 4.5 秒自动关闭和悬停暂停；
 * 带 data 的通知在首次展开后取消后续自动关闭，避免详情看到一半消失。
 */
function showResponseNotice(options: {
  title: string,
  message: unknown,
  data: unknown,
  type: 'warning' | 'error',
}) {
  const hasData = options.data !== null && options.data !== undefined
  if (!hasData) {
    ElNotification({
      title: options.title,
      message: h(ApiResponseNotice, { message: options.message }),
      type: options.type,
      duration: NOTICE_DURATION,
    })
    return
  }

  const noticeClass = `api-response-notification-${++noticeSerial}`
  let notification: { close: () => void } | undefined
  let noticeElement: HTMLElement | undefined
  let timer: number | undefined
  let expanded = false
  let closed = false

  const clearTimer = () => {
    if (undefined !== timer) window.clearTimeout(timer)
    timer = undefined
  }
  const startTimer = () => {
    if (closed || expanded) return
    clearTimer()
    timer = window.setTimeout(() => notification?.close(), NOTICE_DURATION)
  }
  const cleanup = () => {
    closed = true
    clearTimer()
    noticeElement?.removeEventListener('mouseenter', clearTimer)
    noticeElement?.removeEventListener('mouseleave', startTimer)
    noticeElement = undefined
  }

  notification = ElNotification({
    title: options.title,
    message: h(ApiResponseNotice, {
      message: options.message,
      data: options.data,
      onExpand: () => {
        expanded = true
        clearTimer()
      },
    }),
    type: options.type,
    customClass: `api-response-notification ${noticeClass}`,
    // 禁用组件内置计时，由这里控制“展开后取消自动关闭”
    duration: 0,
    onClose: cleanup,
  })

  nextTick(() => {
    if (closed) return
    noticeElement = document.querySelector<HTMLElement>(`.${noticeClass}`) ?? undefined
    noticeElement?.addEventListener('mouseenter', clearTimer)
    noticeElement?.addEventListener('mouseleave', startTimer)
    startTimer()
  })
}

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
          ElementUtil.confirm('登录状态已失效，是否前往登录页面重新登录？').then(() => {
            window.location.reload()
          }).catch(() => {})
        } else if (ApiUtil.succeed(result)) {
          tips.success && ElMessage.success(ApiUtil.message(result))
        } else if (tips.warning) {
          const data = ApiUtil.data(result)
          showResponseNotice({
            title: '状态：' + ApiUtil.code(result),
            message: ApiUtil.message(result),
            data,
            type: 'warning',
          })
        }
        if (ApiUtil.succeed(result)) {
          resolve(result)
        } else {
          reject(result)
        }
      }).catch((error: any) => {
        const payload = error?.response?.data
        const hasPayload = payload !== null && payload !== undefined
        const apiPayload = hasPayload && 'object' === typeof payload ? payload : null
        const data = null !== apiPayload && Object.prototype.hasOwnProperty.call(apiPayload, 'data')
          ? ApiUtil.data(apiPayload)
          : payload
        const message = (apiPayload ? ApiUtil.message(apiPayload) : '')
          || ('string' === typeof payload && payload.trim() ? payload : error?.message)
          || '请求失败'
        const code = error?.response?.status
          ?? (apiPayload ? ApiUtil.code(apiPayload) : undefined)
          ?? 500
        const hasDetail = data !== null && data !== undefined
        tips.error && showResponseNotice({
          title: `请求异常：${code}`,
          message,
          data: hasDetail ? data : undefined,
          type: 'error',
        })
        reject(ApiUtil.result(code, message, hasPayload ? data : error))
      })
    })
  },
  request(tips: any, config: any) {
    const counter = useCounterStore()
    counter.fetching = true
    const result = this.wrapper($axios, tips, config)
    counter.fetching = false
    return result
  },
  get(url: string, params = {}, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'get', url, params }))
  },
  post(url: string, data: any = null, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'post', url, data }))
  },
  form(url: string, data: any = null, tips = {}, config = {}) {
    const form = new FormData()
    for (const key in data) {
      form.append(key, data[key])
    }
    return this.request(tips, Object.assign(config, { method: 'post', url, data: form }))
  },
}
