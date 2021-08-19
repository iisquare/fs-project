import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/catalog/list', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/catalog/tree', param, tips)
  },
  delete (id, tips = {}) {
    return base.post('/catalog/delete', { id }, tips)
  },
  config (tips = {}) {
    return base.post('/catalog/config', {})
  },
  save (param, tips = {}) {
    return base.post('/catalog/save', param, tips)
  }
}
