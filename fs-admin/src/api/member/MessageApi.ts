import api from './Api'

export default {
  list (param: any, tips = {}) {
    return api.post('/message/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return api.post('/message/delete', { ids }, tips)
  }
}
