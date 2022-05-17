import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/source/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/source/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/source/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/source/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/source/save', param, tips)
  }
}
