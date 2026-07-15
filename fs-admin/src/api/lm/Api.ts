import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/lm' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/lm' + url, data, tips, config)
  },
  form (url: string, data = {}, tips = {}, config = {}) {
    return api.form('/lm' + url, data, tips, config)
  }
}
