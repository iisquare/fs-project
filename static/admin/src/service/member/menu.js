import base from '@/core/ProxyBase'

export default {
  list (param, tips = {}) {
    return base.post('/menu/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/menu/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/menu/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/menu/save', param, tips)
  }
}
