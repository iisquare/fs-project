import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/role/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/role/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/role/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/role/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/role/save', param, tips)
  },
  permit (param: any, tips = {}) {
    return base.post('/role/permit', param, tips)
  }
}
