import base from './Api'

export default {
  list(param: any, tips = {}) {
    return base.post('/credit/list', param, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/credit/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/credit/config', {}, tips)
  },
  save(param: any, tips = {}) {
    return base.post('/credit/save', param, tips)
  },
  usage(param: any, tips = {}) {
    return base.post('/credit/usage', param, tips)
  },
  reset(param: any, tips = {}) {
    return base.post('/credit/reset', param, tips)
  },
}
