/** 把 Error、接口返回对象或字符串统一成一句可展示的失败原因 */
export function errorMessage(error: unknown, fallback = '操作失败'): string {
  if (error instanceof Error) return error.message.trim() || fallback
  if (error && typeof error === 'object' && 'message' in error) {
    const message = String((error as { message?: unknown }).message ?? '').trim()
    if (message) return message
  }
  const text = String(error ?? '').trim()
  return text && '[object Object]' !== text ? text : fallback
}

/** 把 axios / 服务端返回的底层报错整理成用户能据此处理的网络提示 */
export function networkErrorMessage(error: unknown, fallback = '网络请求失败'): string {
  const message = errorMessage(error, fallback)
  if (/network error|failed to fetch|timeout|timed out|econn/i.test(message)) {
    return `${fallback}：网络连接异常，请检查网络后重试`
  }
  if (/status code 404/i.test(message)) {
    return `${fallback}：服务地址不可用（404），请检查服务端配置`
  }
  if (/status code 5\d\d/i.test(message)) {
    return `${fallback}：服务暂时不可用，请稍后重试`
  }
  return message
}
