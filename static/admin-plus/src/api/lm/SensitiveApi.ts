import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/sensitive/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/sensitive/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/sensitive/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/sensitive/save', param, tips)
  },
  window (param: any, tips = {}) {
    return base.post('/sensitive/window', param, tips)
  },
  check (param: any, tips = {}) {
    return base.post('/sensitive/check', param, tips)
  },
}
