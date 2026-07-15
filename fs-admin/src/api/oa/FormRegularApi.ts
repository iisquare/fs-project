import base from './Api'

export default {
  all(param = {}, tips = {}) {
    return base.post('/formRegular/all', param, tips)
  },
  info(param: any, tips = {}) {
    return base.post('/formRegular/info', param, tips)
  },
  list(param: any, tips = {}) {
    return base.post('/formRegular/list', param, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/formRegular/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/formRegular/config', {}, tips)
  },
  save(param: any, tips = {}) {
    return base.post('/formRegular/save', param, tips)
  },
  test(param: any, tips = {}) {
    return base.post('/formRegular/test', param, tips)
  },
}