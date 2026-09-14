import base from './Api'

export default {
  config (tips = {}) {
    return base.post('/setting/config', {}, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/setting/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/setting/delete', { ids }, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/setting/save', param, tips)
  }
}
