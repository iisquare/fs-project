import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/formData/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/formData/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/formData/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/formData/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/formData/save', param, tips)
  }
}
