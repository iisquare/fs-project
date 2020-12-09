import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/dictionary/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/dictionary/delete', { ids }, tips)
  },
  unique (param, tips = {}) {
    return base.post('/dictionary/unique', param, tips)
  },
  config (tips = {}) {
    return base.post('/dictionary/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/dictionary/save', param, tips)
  }
}
