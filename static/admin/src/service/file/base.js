import base from '@/core/ServiceBase'

export default {
  get (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/get', {
      app: 'File',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/post', {
      app: 'File',
      uri: url,
      data: data
    }, tips, config)
  }
}
