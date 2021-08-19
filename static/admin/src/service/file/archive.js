import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/archive/list', param, tips)
  },
  delete (id, tips = {}) {
    return base.post('/archive/delete', { id }, tips)
  },
  config (tips = {}) {
    return base.post('/archive/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/archive/save', param, tips)
  }
}
