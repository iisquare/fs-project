import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/diagram/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/diagram/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/diagram/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/diagram/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/diagram/config', {}, tips)
  },
  dagConfig(tips = {}) {
    return base.post('/dag/config', {}, tips)
  },
  diagram(id: any, tips = {}) {
    return base.get('/dag/diagram', { id }, tips)
  },
  run(data: any, tips = {}) {
    return base.post('/dag/run', data, tips)
  },
  debug(data: any, tips = {}) {
    return base.post('/dag/debug', data, tips)
  },
}
