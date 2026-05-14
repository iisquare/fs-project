import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/auth/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/auth/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/auth/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/auth/save', param, tips)
  },
}
