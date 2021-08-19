import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/cite/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/cite/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/cite/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/cite/save', param, tips)
  }
}
