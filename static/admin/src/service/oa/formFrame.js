import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/formFrame/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/formFrame/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/formFrame/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/formFrame/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/formFrame/save', param, tips)
  }
}
