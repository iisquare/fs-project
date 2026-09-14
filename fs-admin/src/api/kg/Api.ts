import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/kg' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/kg' + url, data, tips, config)
  },
  form (url: string, data: any = {}, tips = {}, config = {}) {
    return api.form('/kg' + url, data, tips, config)
  }
}
