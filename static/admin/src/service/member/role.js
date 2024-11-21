import base from './base'

export default {
  info (id, tips = {}) {
    return base.get('/role/info', { id }, tips)
  },
  list (param, tips = {}) {
    return base.post('/role/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/role/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/role/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/role/save', param, tips)
  },
  permit (param, tips = {}) {
    return base.post('/role/permit', param, tips)
  }
}
