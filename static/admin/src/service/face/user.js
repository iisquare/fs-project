import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/user/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/user/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/user/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/user/save', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/user/tree', param, tips)
  }
}
