import base from './Api'

export default {
  list(params: any = {}, tips = {}) {
    return base.post('/dataApi/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataApi/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataApi/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataApi/config', {}, tips)
  },
  test(params: any = {}, tips = {}) {
    return base.post('/dataApi/test', params, tips)
  },
}
