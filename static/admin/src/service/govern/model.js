import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/model/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/model/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/model/delete', param, tips)
  },
  config (tips = {}) {
    return base.post('/model/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/model/save', param, tips)
  }
}
