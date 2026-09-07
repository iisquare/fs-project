import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/favorite/list', param, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/favorite/save', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/favorite/delete', { ids }, tips)
  }
}
