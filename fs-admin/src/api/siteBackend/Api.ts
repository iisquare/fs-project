import api from '@/core/Api'

export default {
  get(url: string, params = {}, tips = {}, config = {}) {
    return api.get('/siteBackend' + url, params, tips, config)
  },
  post(url: string, data = {}, tips = {}, config = {}) {
    return api.post('/siteBackend' + url, data, tips, config)
  },
}
