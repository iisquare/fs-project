import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/visualize/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/visualize/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/visualize/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/visualize/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/visualize/save', param, tips)
  },
  search (param, tips = {}) {
    return base.post('/visualize/search', param, tips)
  }
}
