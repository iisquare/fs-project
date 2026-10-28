import base from './Api'

export default {
  info (data: any, tips: any = {}) {
    return base.post('/source/info', data, tips)
  },
  list (data: any = {}, tips: any = {}) {
    return base.post('/source/list', data, tips)
  },
  save (data: any, tips: any = {}) {
    return base.post('/source/save', data, tips)
  },
  delete (data: any, tips: any = {}) {
    return base.post('/source/delete', data, tips)
  },
  config (tips: any = {}) {
    return base.post('/source/config', {}, tips)
  },
}
