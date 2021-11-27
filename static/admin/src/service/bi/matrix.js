import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/matrix/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/matrix/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/matrix/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/matrix/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/matrix/save', param, tips)
  },
  search (param, tips = {}) {
    return base.post('/matrix/search', param, tips)
  }
}
