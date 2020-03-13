import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/resource/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/resource/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/resource/config', {})
  },
  save (param, tips = {}) {
    return base.post('/resource/save', param, tips)
  }
}
