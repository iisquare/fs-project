import base from '@/core/ProxyBase'

export default {
  list (param, tips = {}) {
    return base.post('/setting/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/setting/delete', { ids }, tips)
  },
  save (param, tips = {}) {
    return base.post('/setting/save', param, tips)
  }
}
