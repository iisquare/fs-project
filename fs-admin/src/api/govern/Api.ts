import api from '@/core/Api'

export default {
  get (url: string, params: any = {}, tips: any = {}, config: any = {}) {
    return api.get('/govern' + url, params, tips, config)
  },
  post (url: string, data: any = {}, tips: any = {}, config: any = {}) {
    return api.post('/govern' + url, data, tips, config)
  },
  form (url: string, data: any = {}, tips: any = {}, config: any = {}) {
    return api.form('/govern' + url, data, tips, config)
  },
}
