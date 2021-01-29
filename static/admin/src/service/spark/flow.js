import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/flow/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/flow/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/flow/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/flow/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/flow/save', param, tips)
  }
}
