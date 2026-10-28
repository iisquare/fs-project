import base from './Api'

export default {
  info (data: any, tips: any = {}) {
    return base.post('/assess/info', data, tips)
  },
  list (data: any = {}, tips: any = {}) {
    return base.post('/assess/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/assess/save', data, tips)
  },
  delete (ids: any, tips: any = {}) {
    return base.post('/assess/delete', { ids }, tips)
  },
  config (tips: any = {}) {
    return base.post('/assess/config', {}, tips)
  },
  check (data: any, tips: any = {}) {
    return base.get('/assess/check', data, tips)
  },
  log (data: any = {}, tips: any = {}) {
    return base.post('/assess/log', data, tips)
  },
  clear (ids: any, tips: any = {}) {
    return base.post('/assess/clear', { ids }, tips)
  },
}
