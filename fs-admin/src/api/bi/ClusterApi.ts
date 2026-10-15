import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/cluster/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/cluster/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/cluster/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/cluster/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/cluster/config', {}, tips)
  },
  options(params: any = {}, tips = {}) {
    return base.post('/cluster/options', params, tips)
  },
  test(data: any, tips = {}) {
    return base.post('/cluster/test', data, tips)
  },
}
