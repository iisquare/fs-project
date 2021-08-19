import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/article/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/article/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/article/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/article/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/article/save', param, tips)
  }
}
