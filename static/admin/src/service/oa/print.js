import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/print/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/print/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/print/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/print/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/print/save', param, tips)
  }
}
