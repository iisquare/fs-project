import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/agent' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/agent' + url, data, tips, config)
  },
  form (url: string, data = {}, tips = {}, config = {}) {
    return api.form('/agent' + url, data, tips, config)
  }
}
