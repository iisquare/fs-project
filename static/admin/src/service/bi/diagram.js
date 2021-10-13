import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/diagram/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/diagram/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/diagram/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/diagram/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/diagram/save', param, tips)
  }
}
