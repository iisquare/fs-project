import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/dashboard/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/dashboard/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/dashboard/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/dashboard/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/dashboard/save', param, tips)
  }
}
