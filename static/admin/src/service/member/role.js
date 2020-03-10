import base from '@/core/ProxyBase'

export default {
  list (param, tips = {}) {
    return base.post('/role/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/role/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/role/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/role/save', param, tips)
  },
  tree (param, tips = {}) {
    return base.post('/role/tree', param, tips)
  }
}
