import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dashboardTemplate/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dashboardTemplate/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dashboardTemplate/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dashboardTemplate/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dashboardTemplate/config', {}, tips)
  },
}
