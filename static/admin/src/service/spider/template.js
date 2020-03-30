import base from './base'

export default {
  plain (id, tips = { success: false, warning: false, error: false }) {
    return base.get('/template/plain', { id }, tips)
  },
  info (param, tips = {}) {
    return base.post('/template/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/template/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/template/delete', { ids }, tips)
  },
  save (param, tips = {}) {
    return base.post('/template/save', param, tips)
  }
}
