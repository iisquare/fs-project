import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/application/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/application/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/application/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/application/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/application/save', param, tips)
  }
}
