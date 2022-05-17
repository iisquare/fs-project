import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/standard/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/standard/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/standard/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/standard/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/standard/save', param, tips)
  }
}
