import api from './Api'

export default {
  list (param: any, tips = {}) {
    return api.post('/user/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return api.post('/user/delete', { ids }, tips)
  },
  config (tips = {}) {
    return api.post('/user/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return api.post('/user/save', param, tips)
  },
  password (param: any, tips = {}) {
    return api.post('/user/password', param, tips)
  }
}
