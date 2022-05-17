import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/assess/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/assess/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/assess/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/assess/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/assess/save', param, tips)
  },
  log (param, tips = {}) {
    return base.post('/assess/log', param, tips)
  },
  clear (ids, tips = {}) {
    return base.post('/assess/clear', { ids }, tips)
  }
}
