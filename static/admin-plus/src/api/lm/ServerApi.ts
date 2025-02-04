import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/server/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/server/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/server/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/server/save', param, tips)
  },
}
