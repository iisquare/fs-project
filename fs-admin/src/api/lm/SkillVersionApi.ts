import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/skillVersion/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/skillVersion/delete', { ids }, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/skillVersion/save', param, tips)
  },
  upload (param: any, tips = {}) {
    return base.form('/skillVersion/upload', param, tips)
  },
  download (param: any) {
    return base.get('/skillVersion/download', param, {})
  },
}
