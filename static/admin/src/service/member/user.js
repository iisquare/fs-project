import base from '@/core/ProxyBase'

export default {
  list (param, tips = {}) {
    return base.post('/user/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/user/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/user/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/user/save', param, tips)
  },
  login (param, tips = {}) {
    if (!param) param = {}
    return base.login(param, tips)
  },
  logout (tips = {}) {
    return base.post('/user/logout', {}, tips)
  },
  tree (param, tips = {}) {
    return base.post('/user/tree', param, tips)
  },
  password (param, tips = {}) {
    return base.post('/user/password', param, tips)
  }
}
