import api from './Api'

export default {
  config (tips = {}) {
    return api.post('/message/config', {}, tips)
  },
  list (param: any, tips = {}) {
    return api.post('/message/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return api.post('/message/delete', { ids }, tips)
  }
}
