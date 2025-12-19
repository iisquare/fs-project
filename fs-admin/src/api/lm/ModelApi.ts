import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/model/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/model/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/model/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/model/save', param, tips)
  },
}
