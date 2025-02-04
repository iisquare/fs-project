import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/client/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/client/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/client/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/client/save', param, tips)
  },
}
