import base from '@/core/ServiceBase'

export default {
  get (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/get', {
      app: 'Spider',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/post', {
      app: 'Spider',
      uri: url,
      data: data
    }, tips, config)
  }
}
