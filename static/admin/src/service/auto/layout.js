import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/layout/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/layout/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/layout/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/layout/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/layout/save', param, tips)
  }
}
