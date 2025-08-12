import base from './Api'

export default {
  info (param: any, tips = {}) {
    return base.post('/formFrame/info', param, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/formFrame/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/formFrame/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/formFrame/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/formFrame/save', param, tips)
  }
}
