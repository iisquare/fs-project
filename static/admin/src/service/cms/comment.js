import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/comment/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/comment/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/comment/config', {}, tips)
  },
  audit (param, tips = {}) {
    return base.post('/comment/audit', param, tips)
  }
}
