import base from './Api'

export default {
  info (data: any, tips: any = {}) {
    return base.post('/standard/info', data, tips)
  },
  list (data: any = {}, tips: any = {}) {
    return base.post('/standard/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/standard/save', data, tips)
  },
  delete (data: any, tips: any = {}) {
    return base.post('/standard/delete', data, tips)
  },
  config (tips: any = {}) {
    return base.post('/standard/config', {}, tips)
  },
}
