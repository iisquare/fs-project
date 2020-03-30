import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/plugin/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/plugin/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/plugin/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/plugin/save', param, tips)
  },
  upload (param, tips = {}) {
    return base.post('/plugin/upload', param, tips)
  }
}
