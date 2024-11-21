import base from './base'

export default {
  info (id, tips = {}) {
    return base.get('/application/info', { id }, tips)
  },
  list (param, tips = {}) {
    return base.post('/application/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/application/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/application/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/application/save', param, tips)
  }
}
