import base from './Api'

export default {
  check (param: any, tips = {}) {
    return base.post('/dataLog/check', param, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/dataLog/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/dataLog/delete', { ids }, tips)
  },
}
