import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/feedback/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/feedback/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/feedback/config', {}, tips)
  },
  audit (param, tips = {}) {
    return base.post('/feedback/audit', param, tips)
  }
}
