import base from '@/core/ServiceBase'

const module = 'lucene'

export default {
  get (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/get', {
      app: module,
      uri: url,
      data: data
    }, tips, config)
  },
  post (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/post', {
      app: module,
      uri: url,
      data: data
    }, tips, config)
  }
}
