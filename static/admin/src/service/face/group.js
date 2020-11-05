import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/group/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/group/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/group/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/group/save', param, tips)
  }
}
