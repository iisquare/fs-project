import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/source/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/source/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/source/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/source/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/source/save', param, tips)
  },
  schema (param, tips = {}) {
    return base.post('/source/schema', param, tips)
  }
}
