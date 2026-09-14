import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/skill/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/skill/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/skill/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/skill/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/skill/save', param, tips)
  },
}
