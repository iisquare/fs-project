import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataset/info', { id }, tips)
  },
  trigger(id: any, tips = {}) {
    return base.post('/dataset/trigger', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataset/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataset/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataset/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataset/config', {}, tips)
  },
}
