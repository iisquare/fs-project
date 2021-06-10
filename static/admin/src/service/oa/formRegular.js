import base from './base'

export default {
  all (param = {}, tips = {}) {
    return base.post('/formRegular/all', param, tips)
  },
  info (param, tips = {}) {
    return base.post('/formRegular/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/formRegular/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/formRegular/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/formRegular/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/formRegular/save', param, tips)
  },
  test (param, tips = {}) {
    return base.post('/formRegular/test', param, tips)
  }
}
