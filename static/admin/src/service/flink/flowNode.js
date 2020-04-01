import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/flowNode/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/flowNode/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/flowNode/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/flowNode/save', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/flowNode/tree', param, tips)
  }
}
