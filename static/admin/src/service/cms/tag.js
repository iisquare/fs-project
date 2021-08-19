import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/tag/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/tag/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/tag/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/tag/save', param, tips)
  }
}
