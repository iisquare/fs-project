import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/photo/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/photo/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/photo/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/photo/save', param, tips)
  }
}
