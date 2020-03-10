import base from '@/core/ServiceBase'

export default {
  get (url, tips = {}, config = {}) {
    return base.post('/proxy/get', {
      app: 'admin',
      uri: url,
      data: {}
    }, tips, config)
  },
  post (url, data = null, tips = {}, config = {}) {
    return base.post('/proxy/post', {
      app: 'admin',
      uri: url,
      data: {}
    }, tips, config)
  },
  login (data = {}, tips = {}, config = {}) {
    return base.post('/proxy/login', data, tips, config)
  }
}
