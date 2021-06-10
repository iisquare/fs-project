import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/dictionary/list', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/dictionary/tree', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/dictionary/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/dictionary/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/dictionary/save', param, tips)
  },
  available (param, tips = {}) {
    return base.post('/dictionary/available', param, tips)
  }
}
