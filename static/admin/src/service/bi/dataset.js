import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/dataset/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/dataset/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/dataset/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/dataset/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/dataset/save', param, tips)
  }
}
